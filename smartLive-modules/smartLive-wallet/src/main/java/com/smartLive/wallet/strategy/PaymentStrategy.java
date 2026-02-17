package com.smartLive.wallet.strategy;

import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;

/**
 * 支付策略接口
 *
 * @author smartLive
 */
public interface PaymentStrategy {

    /**
     * 获取支付方式
     *
     * @return wechat/alipay
     */
    String getPayMethod();

    /**
     * 统一下单
     *
     * @param record 支付记录
     * @param dto    前端请求参数
     * @return 下单响应
     */
    UnifiedPayVO unifiedOrder(PaymentRecord record, UnifiedPayDTO dto);

    /**
     * 查询支付状态
     *
     * @param record 支付记录
     * @return 状态VO
     */
    PayStatusVO queryPayStatus(PaymentRecord record);
}
