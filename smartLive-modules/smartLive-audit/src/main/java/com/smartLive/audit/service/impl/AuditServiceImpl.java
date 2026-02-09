package com.smartLive.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.audit.mapper.AuditTaskMapper;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.audit.strategy.AuditStrategyFactory;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审核服务实现类
 */
@Service
public class AuditServiceImpl extends ServiceImpl<AuditTaskMapper, AuditTask> implements IAuditService {

    @Autowired
    private AuditStrategyFactory auditStrategyFactory;
    @Autowired
    private RemoteAppUserService remoteAppUserService;

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
            // Convert to VO
            AuditTaskVO vo = AuditTaskVO.fromEntity(task);

            // Apply Strategy for Risk Calculation
            AuditStrategy strategy = auditStrategyFactory.getStrategy(task.getBizType());
            vo.setIsHighRisk(strategy.isHighRisk(task));
            // Optional: Format content if needed
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

        // Convert to VO
        AuditTaskVO vo = AuditTaskVO.fromEntity(task);

        // Apply Strategy
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
    public boolean auditAction(Long id, Integer status, String reason) {
        AuditTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("Audit task not found for id: " + id);
        }

        task.setStatus(status);
        task.setReason(reason);

        // Call Strategy to handle business callback
        AuditStrategy strategy = auditStrategyFactory.getStrategy(task.getBizType());
        if (strategy != null) {
            strategy.handleAuditResult(task.getBizId(), status, reason);
        }

        return this.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAuditTask(AuditMessage auditMessage) {
        AuditTask auditTask = new AuditTask();
        auditTask.setBizId(auditMessage.getBizId());
        auditTask.setBizType(auditMessage.getBizType());
        auditTask.setSubmitterId(auditMessage.getSubmitterId());
        auditTask.setAuditContent(auditMessage.getAuditContent());
        auditTask.setStatus(0); // 0-待审
        auditTask.setCreateTime(new Date());
        auditTask.setUpdateTime(new Date());

        this.save(auditTask);
    }
}
