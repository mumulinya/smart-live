package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public  class VoucherResourceStrategy implements ResourceStrategy<VoucherDTO> {
    @Autowired
    private RemoteVoucherService remoteVoucherService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.VOUCHER_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<VoucherDTO> getResourceList(List<Long> sourceIdList) {
        List<VoucherDTO> voucherDTOList  = remoteVoucherService.getVoucherListByIds(sourceIdList);
        if (voucherDTOList == null) {
            return null;
        }
        return voucherDTOList;
    }
}
