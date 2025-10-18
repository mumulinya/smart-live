package com.smartLive.order.listener;

import cn.hutool.core.bean.BeanUtil;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.marketing.api.dto.VoucherDTO;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.IVoucherOrderService;
import com.smartLive.order.service.impl.VoucherOrderServiceImpl;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderVoucherListener {

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;
    //秒杀券监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.ORDER_SECKILL_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.ORDER_EXCHANGE_NAME),
            key = MqConstants.ORDER_SECKILL_ROUTING
    ))
    public void handleSeckillVoucherOrder(VoucherOrder voucherOrder){
        voucherOrderService.handleVoucherOrder(voucherOrder);
    }

    //普通优惠券监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.ORDER_BUY_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.ORDER_EXCHANGE_NAME),
            key = MqConstants.ORDER_BUY_ROUTING
    ))
    public void handleBuyVoucherOrder(VoucherOrder voucherOrder){
        voucherOrderService.save(voucherOrder);
    }
}
