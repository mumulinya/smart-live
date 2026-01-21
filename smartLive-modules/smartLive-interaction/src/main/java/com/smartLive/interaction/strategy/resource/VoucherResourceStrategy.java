package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.interaction.domain.VO.VoucherVO;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public  class VoucherResourceStrategy implements ResourceStrategy<VoucherVO> {
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
    public List<VoucherVO> getResourceList(List<Long> sourceIdList) {
        List<VoucherDTO> voucherDTOList  = remoteVoucherService.getVoucherListByIds(sourceIdList);
        if (voucherDTOList == null) {
            return null;
        }
        List<VoucherVO> voucherVOList = voucherDTOList.stream().map(voucherDTO -> {
            VoucherVO voucherVO = new VoucherVO();
            BeanUtils.copyProperties(voucherDTO, voucherVO);
            return voucherVO;
        }).toList();
        return voucherVOList;
    }
}
