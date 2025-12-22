package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.marketing.api.RemoteMarketingService;
import com.smartLive.marketing.api.dto.VoucherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("VoucherResourceStrategy")
public abstract  class VoucherResourceStrategy implements ResourceStrategy {
    @Autowired
    private RemoteMarketingService remoteMarketingService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public String getType() {
        return ResourceTypeEnum.VOUCHER_RESOURCE.getBizDomain()+"ResourceStrategy";
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ResourceVO> getResourceList(List<Long> sourceIdList) {
        R<List<VoucherDTO>> voucherSuccess = remoteMarketingService.getVoucherListByIds(sourceIdList);
        if (voucherSuccess.getCode() != 200) {
            return null;
        }
        List<VoucherDTO> voucherDTOList = voucherSuccess.getData();
        List<ResourceVO> resourceVOList = voucherDTOList.stream().map(voucherDTO -> ResourceVO.builder()
                .id(voucherDTO.getId())
                .shopName(voucherDTO.getShopName())
//                .distance(voucherDTO.getDistance())
                .originalPrice(voucherDTO.getPayValue())
                .presentPrice(voucherDTO.getActualValue().toString())
                .build()
        ).collect(Collectors.toList());
        return resourceVOList;
    }
}
