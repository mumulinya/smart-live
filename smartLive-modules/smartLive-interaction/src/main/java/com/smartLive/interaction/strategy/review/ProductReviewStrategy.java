package com.smartLive.interaction.strategy.review;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductReviewStrategy implements ReviewStrategy {

    private final RemoteProductService remoteProductService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.PRODUCT_RESOURCE.getCode();
    }

    /**
     * 设置评价来源名称
     *
     * @param reviewVO
     * @return
     */
    @Override
    public List<ReviewVO> setSourceName(List<ReviewVO> reviewVOs) {
        //获取源id集合
        List<Long> list = reviewVOs.stream().map(ReviewVO::getSourceId).distinct().toList();
        List<ProductDTO> productList = remoteProductService.getProductListByIds(list);
        //将源id和源名称对应起来
        Map<Long, String> result = productList.stream()
                .collect(Collectors.toMap(
                        ProductDTO::getId,
                        ProductDTO::getName
                ));
        reviewVOs.forEach(reviewVO -> {
            reviewVO.setSourceName(result.get(reviewVO.getSourceId()));
        });
        return reviewVOs;
    }

    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用商品服务的批量更新接口
        remoteProductService.updateReviewCountBatch(updateMap);
    }
}