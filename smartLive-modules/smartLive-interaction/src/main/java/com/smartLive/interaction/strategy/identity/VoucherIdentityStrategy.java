package com.smartLive.interaction.strategy.identity;

import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.dto.VoucherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public  class VoucherIdentityStrategy implements IdentityStrategy<VoucherDTO> {
    @Autowired
    private RemoteVoucherService remoteVoucherService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return IdentityTypeEnum.VOUCHER_IDENTITY.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<VoucherDTO> getFollowList(List<Long> sourceIdList) {
        List<VoucherDTO> voucherDTOList  = remoteVoucherService.getVoucherListByIds(sourceIdList);
        if (voucherDTOList == null) {
            return null;
        }
        return voucherDTOList;
    }
}
