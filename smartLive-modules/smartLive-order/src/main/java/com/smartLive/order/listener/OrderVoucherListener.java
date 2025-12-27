package com.smartLive.order.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.impl.VoucherOrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OrderVoucherListener {

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    //秒杀券监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.ORDER_SECKILL_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.ORDER_DEAD_LETTER_ROUTING),
                            //设置惰性队列
                            @Argument(name = "x-queue-mode", value = "lazy")
                    }
            ),
            exchange = @Exchange(name = MqConstants.ORDER_EXCHANGE_NAME),
            key = MqConstants.ORDER_SECKILL_ROUTING
    ))
    public void handleSeckillVoucherOrder(VoucherOrder voucherOrder) {
        //判断当前订单是否重复创建
        if(voucherOrderService.getById(voucherOrder.getId())!=null){
            log.error("订单已存在");
            return;
        }
        voucherOrderService.handleVoucherOrder(voucherOrder);
    }

    //普通优惠券监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.ORDER_BUY_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = MqConstants.ORDER_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = MqConstants.ORDER_EXCHANGE_NAME),
            key = MqConstants.ORDER_BUY_ROUTING
    ))
    public void handleBuyVoucherOrder(VoucherOrder voucherOrder){
        log.info("开始处理订单信息: {}", voucherOrder);
        //判断当前订单是否重复创建
        if(voucherOrderService.getById(voucherOrder.getId())!=null){
            log.error("订单已存在");
            return;
        }
        //创建订单
        boolean save = voucherOrderService.save(voucherOrder);
        // 模拟业务逻辑...
//        int i = 1 / 0; // 模拟异常
        if(!save){
            //创建失败
            log.error("创建订单失败");
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
    /**
     * 监听死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.ORDER_DEAD_LETTER_QUEUE, durable = "true"), // 死信队列名
            exchange = @Exchange(value = MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
            key = MqConstants.ORDER_DEAD_LETTER_ROUTING
    ))
    public void handleDeadLetter(VoucherOrder voucherOrder, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("死信队列收到订单信息为: {}", voucherOrder);
        // TODO: 保存到数据库异常表
        channel.basicAck(deliveryTag, false);
    }
}
