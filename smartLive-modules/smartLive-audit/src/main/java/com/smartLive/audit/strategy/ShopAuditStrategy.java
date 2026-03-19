package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.system.api.RemoteUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 店铺审核策略
 */
@Component
public class ShopAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: 添加店铺高风险判断逻辑
        return false;
    }

    /**
     * 处理审核结果并更新店铺状态
     *
     * @param targetId 目标店铺编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否更新成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        return remoteShopService.updateShopStatus(targetId, status, reason);
    }

    /**
     * 获取提交人名称
     *
     * @param submitterId 提交人编号
     * @return 提交人名称
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteUserService.getUserById(submitterId).getNickName();
    }
}
