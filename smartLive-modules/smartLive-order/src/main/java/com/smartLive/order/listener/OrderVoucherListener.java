package com.smartLive.order.listener;

import cn.hutool.core.bean.BeanUtil;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.utils.MqMessageSendUtils;
import com.smartLive.marketing.api.dto.VoucherDTO;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.IVoucherOrderService;
import com.smartLive.order.service.impl.VoucherOrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderVoucherListener {

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
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
        //创建订单
        boolean save = voucherOrderService.save(voucherOrder);
        if(!save){
            //创建失败
            log.error("创建订单失败");
            return;
        }else{
            //发送延迟消息，检测订单支付状态
            MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
        }
    }

    //支付延迟监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.ORDER_DELAY_QUEUE),
            exchange = @Exchange(name = MqConstants.ORDER_DELAY_EXCHANGE_NAME,
                    type = "x-delayed-message", // 使用 x-delayed-message 类型交换机
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct") // 指定路由类型
                    ),
            key = MqConstants.ORDER_DELAY_ROUTING
    ))
    public void handlePayOrder(Long id){
        VoucherOrder voucherOrder = voucherOrderService.getById(id);
        //检测订单状态，判断订单是否支付
        if(voucherOrder.getStatus()== OrderStatusConstants.PAID||voucherOrder==null){
            log.info("订单不存在或者订单已经支付");
           //订单不存在或者订单已经支付
            return;
        }
        //订单未支付
        if(voucherOrder.getStatus()== OrderStatusConstants.UNPAID){
            log.info("订单未支付，取消订单");
            //取消订单,恢复库存
            voucherOrderService.cancel(id);
        }
    }
}
