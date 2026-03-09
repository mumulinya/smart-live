package com.smartLive.interaction.strategy.review;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.service.IReviewService;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ShopReviewStrategy implements ReviewStrategy {

    private final RemoteShopService remoteShopService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.SHOP_RESOURCE.getCode();
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
        List<ShopDTO> productList = remoteShopService.getShopList(list);
        //
        Map<Long, String> result = productList.stream()
                .collect(Collectors.toMap(
                        ShopDTO::getId,
                        ShopDTO::getName
                ));
        reviewVOs.forEach(reviewVO -> {
            reviewVO.setSourceName(result.get(reviewVO.getSourceId()));
        });
        return reviewVOs;
    }

    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用店铺服务的批量更新接口
        remoteShopService.updateReviewCountBatch(updateMap);
    }
}