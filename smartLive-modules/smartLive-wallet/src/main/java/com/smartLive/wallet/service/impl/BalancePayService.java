package com.smartLive.wallet.service.impl;

import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderPaidStatsMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.api.DTO.OrderDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.wallet.domain.dto.PaymentBusinessResult;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 站内余额支付专用服务
 * 专门处理通过用户账户可用余额抵扣订单金额的逻辑，包含余额预扣、动账记录生成及订单状态异步通知。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Service
public class BalancePayService {

    @Autowired
    private WalletPaymentTxService walletPaymentTxService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    /**
     * 执行余额支付逻辑
     * 1. 落地本地支付单据记录 (状态直接为成功)
     * 2. 调用钱包服务执行原子扣减并记录账单流水
     * 3. 同步调用订单模块标记支付成功
     * 
     * @param userId 支付用户 ID
     * @param dto 支付请求参数
     */
    public void pay(Long userId, UnifiedPayDTO dto) {
        PaymentBusinessResult result = walletPaymentTxService.payOrderByBalance(userId, dto);
        publishOrderPaidStatsIfNecessary(result);
    }

    /**
     * 在余额支付主事务成功后发送销量统计消息。
     * 这一步不纳入 Seata，只承担最终一致性的缓存统计职责。
     *
     * @param result 支付主事务执行结果
     */
    private void publishOrderPaidStatsIfNecessary(PaymentBusinessResult result) {
        if (result == null || !result.shouldPublishOrderPaidStats()) {
            return;
        }

        try {
            OrderDTO orderDTO = remoteOrderService.getOrderById(result.getOrderId());
            OrderPaidStatsMessage message = new OrderPaidStatsMessage();
            message.setOrderId(result.getOrderId());
            message.setUserId(result.getUserId());
            message.setPayType(result.getPayType());
            message.setMessageKey(buildMessageKey(result.getOrderId(), result.getPayType()));
            if (orderDTO != null) {
                message.setSourceId(orderDTO.getSourceId());
                message.setVerifyShopId(orderDTO.getVerifyShopId());
                message.setAmount(orderDTO.getAmount());
                if (message.getUserId() == null) {
                    message.setUserId(orderDTO.getUserId());
                }
            } else {
                log.warn("余额支付成功后未取到订单详情，按最小消息发送统计事件, orderId={}", result.getOrderId());
            }

            mqMessageSendUtils.sendMqMessage(
                    OrderMqConstants.ORDER_PAID_STATS_EXCHANGE,
                    OrderMqConstants.ORDER_PAID_STATS_ROUTING_KEY,
                    message
            );
            log.info("余额支付后已发送订单支付统计消息, orderId={}, payType={}", result.getOrderId(), result.getPayType());
        } catch (Exception e) {
            log.error("余额支付后的统计消息发送失败，主交易已提交, orderId={}", result.getOrderId(), e);
        }
    }

    /**
     * 构造订单支付统计消息的业务幂等 Key。
     *
     * @param orderId 订单ID
     * @param payType 支付方式
     * @return 幂等 Key
     */
    private String buildMessageKey(Long orderId, Integer payType) {
        return  OrderMqConstants.ORDER_PAID_STATS_ROUTING_KEY+":" + orderId + ":" + payType;
    }
}
