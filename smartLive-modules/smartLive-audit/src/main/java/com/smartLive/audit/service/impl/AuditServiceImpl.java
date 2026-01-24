package com.smartLive.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.audit.mapper.AuditTaskMapper;
import com.smartLive.audit.service.IAuditService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审核服务实现类
 */
@Service
public class AuditServiceImpl extends ServiceImpl<AuditTaskMapper, AuditTask> implements IAuditService {

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
        return list.stream().map(AuditTaskVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public boolean auditAction(Long id, Integer status, String reason) {
        AuditTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("Audit task not found for id: " + id);
        }
        
        task.setStatus(status);
        task.setReason(reason);
        // 这里可以扩展添加审核后回调业务服务的逻辑
        // 例如：MessagingService.sendAuditResult(...)
        
        return this.updateById(task);
    }
}
