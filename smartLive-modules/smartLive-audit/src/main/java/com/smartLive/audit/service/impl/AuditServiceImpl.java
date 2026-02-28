package com.smartLive.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.audit.mapper.AuditTaskMapper;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.audit.strategy.AuditStrategyFactory;
import com.smartLive.chat.api.dto.SystemNoticeCreateDTO;
import com.smartLive.common.core.constant.mq.ChatMqConstants;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.utils.SensitiveWordUtil;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.user.api.RemoteAppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private SensitiveWordUtil sensitiveWordUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;

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
            strategy.handleAuditResult(task.getBizId(), status, reason);
        }
        if (updated && status != null && status.equals(AuditStatusEnum.REJECT.getCode())) {
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
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                    ChatMqConstants.SYSTEM_NOTICE_EXCHANGE,
                    ChatMqConstants.SYSTEM_NOTICE_ROUTING,
                    createDTO);
        } catch (Exception e) {
            log.error("create reject system notice failed, auditTaskId={}", task.getId(), e);
        }
    }

    private String buildRejectTitle(Integer bizType) {
        if (bizType == null) {
            return "\u5ba1\u6838\u672a\u901a\u8fc7";
        }
        return switch (bizType) {
            case 1 -> "\u7528\u6237\u4fe1\u606f\u5ba1\u6838\u672a\u901a\u8fc7";
            case 2 -> "\u5e97\u94fa\u4fe1\u606f\u5ba1\u6838\u672a\u901a\u8fc7";
            case 3 -> "\u7b14\u8bb0\u5ba1\u6838\u672a\u901a\u8fc7";
            case 4 -> "\u4f18\u60e0\u5238\u5ba1\u6838\u672a\u901a\u8fc7";
            case 5 -> "\u8bc4\u8bba\u5ba1\u6838\u672a\u901a\u8fc7";
            case 7 -> "\u8bc4\u4ef7\u5ba1\u6838\u672a\u901a\u8fc7";
            default -> "\u5ba1\u6838\u672a\u901a\u8fc7";
        };
    }

    /**
     * 处理审核任务
     *
     * @param auditMessage 审核消息
     */
    @Override
    public void handleAudit(AuditMessage auditMessage) {
        Long auditTaskId = createAuditTask(auditMessage);
       if (auditTaskId != null) {
           String content = extractTextFromMap(auditMessage.getAuditContent());
           boolean contains = sensitiveWordUtil.hasSensitiveWord(content);
           // 如果包含敏感词，说明不安全
           if (contains) {
               //找出敏感词
               List<String> all = sensitiveWordUtil.findAll(content);
               log.info("contains sensitive words: {}", all);
               auditAction(auditTaskId, AuditStatusEnum.REJECT.getCode(), "你的消息包含敏感词："+String.join(",", all));
           }
        }
    }
    /**
     * 【辅助方法】智能提取 Map 中的文本
     * 针对不同的业务，字段名可能不同，这里统一处理
     */
    private String extractTextFromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 1. 尝试提取标题 (针对博客/文章)
        if (map.containsKey("title")) {
            sb.append(map.get("title")).append("\n"); // 加换行符，防止标题和正文连在一起造成误判
        }

        // 2. 尝试提取内容 (针对博客/评论)
        if (map.containsKey("content")) {
            sb.append(map.get("content"));
        }

        // 3. 尝试提取昵称/签名 (针对用户修改信息)
        if (map.containsKey("nickname")) {
            sb.append(map.get("nickname"));
        }
        if (map.containsKey("introduce")) {
            sb.append(map.get("introduce"));
        }
        // 4. 尝试提取副标题/规则 (针对店铺/团购)
        if (map.containsKey("title")) {
            sb.append(map.get("title"));
        }
        if (map.containsKey("subTitle")) {
            sb.append(map.get("subTitle"));
        }
        if (map.containsKey("rules")) {
            sb.append(map.get("rules"));
        }
        // 5. 尝试提取名称 (针对代店铺)
        if (map.containsKey("name")) {
            sb.append(map.get("name"));
        }
        return sb.toString();
    }
}
