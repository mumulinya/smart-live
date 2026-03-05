package com.smartLive.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.audit.chain.AuditDecision;
import com.smartLive.audit.chain.AuditProcessChain;
import com.smartLive.audit.chain.AuditProcessContext;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.audit.mapper.AuditTaskMapper;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.audit.strategy.AuditStrategyFactory;
import com.smartLive.chat.api.dto.SystemNoticeCreateDTO;
import com.smartLive.common.core.constant.mq.ChatMqConstants;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Audit service implementation
 */
@Service
@Slf4j
public class AuditServiceImpl extends ServiceImpl<AuditTaskMapper, AuditTask> implements IAuditService {

    @Autowired
    private AuditStrategyFactory auditStrategyFactory;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private AuditProcessChain auditProcessChain;

    @Override
    public List<AuditTaskVO> selectAuditList(AuditTask auditTask) {
        LambdaQueryWrapper<AuditTask> lqw = Wrappers.lambdaQuery();
        if (auditTask.getStatus() != null) {
            lqw.eq(AuditTask::getStatus, auditTask.getStatus());
        }
        if (auditTask.getBizType() != null) {
            lqw.eq(AuditTask::getBizType, auditTask.getBizType());
        }
        lqw.orderByDesc(AuditTask::getCreateTime);

        List<AuditTask> list = this.list(lqw);

        return list.stream().map(task -> {
            AuditTaskVO vo = AuditTaskVO.fromEntity(task);

            AuditStrategy strategy = auditStrategyFactory.getStrategy(task.getBizType());
            vo.setIsHighRisk(strategy.isHighRisk(task));
            if (task.getAuditContent() != null) {
                vo.setAuditContent(task.getAuditContent());
            }
            String submitterName = strategy.getSubmitterName(task.getSubmitterId());
            vo.setSubmitterName(submitterName);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public AuditTaskVO getAuditDetail(Long id) {
        AuditTask task = this.getById(id);
        if (task == null) {
            return null;
        }

        AuditTaskVO vo = AuditTaskVO.fromEntity(task);

        AuditStrategy strategy = auditStrategyFactory.getStrategy(task.getBizType());
        if (strategy != null) {
            vo.setIsHighRisk(strategy.isHighRisk(task));
            if (task.getAuditContent() != null) {
                vo.setAuditContent(task.getAuditContent());
            }
        }

        return vo;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAuditTask(AuditMessage auditMessage) {
        AuditTask auditTask = new AuditTask();
        auditTask.setBizId(auditMessage.getBizId());
        auditTask.setBizType(auditMessage.getBizType());
        auditTask.setSubmitterId(auditMessage.getSubmitterId());
        auditTask.setAuditContent(auditMessage.getAuditContent());
        auditTask.setStatus(0);
        auditTask.setCreateTime(new Date());
        auditTask.setUpdateTime(new Date());
        boolean save = this.save(auditTask);
        if (save) {
            return auditTask.getId();
        }
        return null;
    }
    @Override
    public boolean auditAction(Long id, Integer status, String reason) {
        AuditTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("Audit task not found for id: " + id);
        }

        task.setStatus(status);
        task.setReason(reason);

        AuditStrategy strategy = auditStrategyFactory.getStrategy(task.getBizType());
        boolean updated = this.updateById(task);
        if (updated && strategy != null) {
            strategy.handleAuditResult(task.getBizId(), AuditStatusEnum.MANUAL_REJECT.getCode(), reason);
        }
        if (updated && AuditStatusEnum.isRejected(status)) {
            createRejectSystemNotice(task, reason);
        }

        return updated;
    }

    private void createRejectSystemNotice(AuditTask task, String reason) {
        if (task.getSubmitterId() == null) {
            return;
        }
        try {
            String title = buildRejectTitle(task.getBizType());
            SystemNoticeCreateDTO createDTO = new SystemNoticeCreateDTO();
            createDTO.setUserId(task.getSubmitterId());
            createDTO.setSourceType(task.getBizType());
            createDTO.setSourceId(task.getBizId());
            createDTO.setAction("AUDIT_REJECT");
            createDTO.setTitle(title);
            createDTO.setExtraData(task.getAuditContent());
            createDTO.setRejectReason(StringUtils.isNotBlank(reason) ? reason : "");
            // 通过 MQ 异步发送审核拒绝的系统通知，解耦 audit 模块与 chat 模块
            mqMessageSendUtils.sendMqMessage(
                    ChatMqConstants.SYSTEM_NOTICE_EXCHANGE,
                    ChatMqConstants.SYSTEM_NOTICE_ROUTING,
                    createDTO);
        } catch (Exception e) {
            log.error("create reject system notice failed, auditTaskId={}", task.getId(), e);
        }
    }

    private String buildRejectTitle(Integer bizType) {
        if (bizType == null) {
            return "审核未通过";
        }
        return switch (bizType) {
            case 1 -> "用户信息审核未通过";
            case 2 -> "店铺信息审核未通过";
            case 3 -> "笔记审核未通过";
            case 4 -> "优惠券审核未通过";
            case 5 -> "评论审核未通过";
            case 7 -> "评价审核未通过";
            default -> "审核未通过";
        };
    }

    /**
     * 处理审核任务
     *
     * @param auditMessage 审核消息
     */
    @Override
    public void handleAudit(AuditMessage auditMessage) {
        // 第一步：无论后续是否命中规则，先创建审核任务，保证全链路可追踪
        Long auditTaskId = createAuditTask(auditMessage);
        if (auditTaskId == null) {
            log.error("创建审核任务失败，消息内容：{}", auditMessage);
            return;
        }

        // 第二步：执行责任链（敏感词 -> AI占位 -> 人工待审）
        AuditProcessContext context = new AuditProcessContext(auditTaskId, auditMessage);
        try {
            auditProcessChain.execute(context);
        } catch (Exception e) {
            // 链路异常兜底：任务保留为待审，进入人工处理
            context.waitManual("审核链执行异常，已转人工审核");
            log.error("审核责任链执行异常，taskId={}", auditTaskId, e);
        }

        // 第三步：根据责任链决策更新任务状态
        applyAuditDecision(context);
    }

    /**
     * 根据责任链结果推进审核状态
     *
     * @param context 审核上下文
     */
    private void applyAuditDecision(AuditProcessContext context) {
        if (context == null || context.getAuditTaskId() == null) {
            return;
        }

        if (context.getDecision() == AuditDecision.REJECT) {
            String reason = StringUtils.isNotBlank(context.getReason()) ? context.getReason() : "内容不符合发布规范";
            auditAction(context.getAuditTaskId(), AuditStatusEnum.AUTO_REJECT.getCode(), reason);
            return;
        }

        if (context.getDecision() == AuditDecision.PASS) {
            String reason = StringUtils.isNotBlank(context.getReason()) ? context.getReason() : "自动审核通过";
            auditAction(context.getAuditTaskId(), AuditStatusEnum.PASS.getCode(), reason);
            return;
        }

        // WAIT_MANUAL 或 CONTINUE 都保持 WAITING(0)，由人工审核页面继续处理
        log.info("审核任务进入人工审核队列，taskId={}, reason={}", context.getAuditTaskId(), context.getReason());
    }
}
