package com.smartLive.shop.listener;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.shop.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShopListener {

    @Autowired
    private IShopService shopService;
    //评论监听器
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.SHOP_COMMENT_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.SHOP_EXCHANGE_NAME),
            key = MqConstants.SHOP_COMMENT_ROUTING
    ))
    public void handleUpdateCommentCount(Long shopId ){
        log.info("评论监听器收到消息：{}",shopId);
        shopService.updateCommentById(shopId);
    }
}
