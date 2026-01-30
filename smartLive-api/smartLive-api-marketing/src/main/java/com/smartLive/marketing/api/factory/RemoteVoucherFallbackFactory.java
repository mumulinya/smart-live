package com.smartLive.marketing.api.factory;

import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteVoucherFallbackFactory implements FallbackFactory<RemoteVoucherService> {
    @Override
    public RemoteVoucherService create(Throwable throwable) {
        return new RemoteVoucherService() {
            @Override
            public Boolean updateVoucherById(Long id) {
                log.error("秒杀券更新失败:{}", throwable.getMessage());
                return false;
            }

            @Override
            public VoucherDTO getVoucherById(Long voucherId) {
                log.error("查询秒杀券失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 恢复库存
             *
             * @param voucherId
             */
            @Override
            public Boolean recoverVoucherStock(Long voucherId) {
                log.error("恢复库存失败:{}", throwable.getMessage());
                return false;
            }

            /**
             * 获取代金券总数
             */
            @Override
            public Integer getCouponTotal() {
                log.error("查询代金券总数失败:{}", throwable.getMessage());
                return 0;
            }

            /**
             * 秒杀优惠券
             *
             * @param voucherId
             * @param userId
             */
            @Override
            public Long seckillVoucher(Long voucherId, Long userId) {
                log.error("秒杀优惠券失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 购买优惠券
             *
             * @param voucherId
             * @param userId
             */
            @Override
            public Long buyVoucher(Long voucherId, Long userId) {
                log.error("购买优惠券失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 获取优惠券列表
             *
             * @param sourceIdList
             */
            @Override
            public List<VoucherDTO> getVoucherListByIds(List<Long> sourceIdList) {
                log.error("获取优惠券列表失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 批量更新点赞数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新点赞数失败:{}", throwable.getMessage());
                return false;
            }

            /**
             * 获取点赞数
             *
             * @param sourceId
             */
            @Override
            public Integer getVoucherStarCount(Long sourceId) {
                log.error("获取优惠券收藏数失败:{}", throwable.getMessage());
                return 0;
            }

            /**
             * 批量更新评价数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新评价数失败:{}", throwable.getMessage());
                return false;
            }
        };
    }
}
