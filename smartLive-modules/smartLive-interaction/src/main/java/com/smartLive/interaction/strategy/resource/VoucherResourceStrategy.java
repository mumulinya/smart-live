package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.interaction.domain.VO.VoucherVO;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public VoucherVO getResourceById(Long sourceId) {
        VoucherDTO voucherDTO = remoteVoucherService.getVoucherById(sourceId);
        if (voucherDTO != null) {
            VoucherVO voucherVO = new VoucherVO();
            BeanUtils.copyProperties(voucherDTO, voucherVO);
            return voucherVO;
        }
        return null;
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        VoucherDTO voucherDTO = remoteVoucherService.getVoucherById(sourceId);
        HashMap<String,String> map = new HashMap<>();
        map.put("title",voucherDTO.getTitle());
        map.put("images",voucherDTO.getShopImages());
        return map;
    }
}
