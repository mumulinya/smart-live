package com.smartLive.marketing.service.impl;

import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.mapper.SeckillVoucherMapper;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 修改秒杀优惠券信息
     *
     * @param voucherId
     * @return
     */
    @Override
    public R<Boolean> updateSeckillVoucherByVoucherId(Long voucherId) {
        boolean update = update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                //防止超卖
                .gt("stock", 0)
                .update();
        return R.ok(update);
    }

    /**
     * 恢复秒杀券优惠券库存
     *
     * @param voucherId
     * @return
     */
    @Override
    public R<Boolean> recoverVoucherStock(Long voucherId) {
        boolean update = update().setSql("stock = stock + 1")
                .eq("voucher_id", voucherId)
                .update();
        if( update){
            //恢复redis的库存
            String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
            stringRedisTemplate.opsForValue().increment(stockKey, 1);
        }
        return R.ok( update);
    }
}
