package com.smartLive.marketing.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.domain.R;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.domain.Voucher;

import java.util.List;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
public interface ISeckillVoucherService extends IService<SeckillVoucher> {

    /**
     * 修改秒杀优惠券信息
     * @param voucherId
     * @return
     */
    R<Boolean> updateSeckillVoucherByVoucherId(Long voucherId);

}
