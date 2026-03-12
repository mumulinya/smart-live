package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.interaction.domain.VO.ProductVO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.product.api.DTO.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class ProductResourceStrategy implements ResourceStrategy<ProductVO> {
    @Autowired
    private RemoteProductService remoteProductService;
    /**
     * 策略标识
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.PRODUCT_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ProductVO> getResourceList(List<Long> sourceIdList) {
        List<ProductDTO> productDTOList  = remoteProductService.getProductListByIds(sourceIdList);
        if (productDTOList == null) {
            return null;
        }
        
        List<ProductVO> productVOList = productDTOList.stream().map(productDTO -> {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(productDTO, productVO);
            return productVO;
        }).toList();
        return productVOList;
    }

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public ProductVO getResourceById(Long sourceId) {
        ProductDTO productDTO = remoteProductService.getProductById(sourceId);
        if (productDTO != null) {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(productDTO, productVO);
            return productVO;
        }
        return null;
    }

    /**
     * 获取资源id
     *
     * @param data
     * @return
     */
    @Override
    public Long getResourceId(ProductVO data) {
        return data.getId();
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        ProductDTO productDTO = remoteProductService.getProductById(sourceId);
        HashMap<String,String> map = new HashMap<>();
        map.put("title",productDTO.getName()); // map name directly
        map.put("images",productDTO.getCoverImg());
        return map;
    }
}
