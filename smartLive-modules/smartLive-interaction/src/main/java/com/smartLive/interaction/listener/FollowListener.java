package com.smartLive.interaction.listener;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.interaction.service.IFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class FollowListener {

    @Autowired
    private IFollowService followService;
    @Autowired
    private ExecutorService executorService;
    //普通数据推送
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.INTERACT_FEED_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.INTERACT_FEED_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = {
                    MqConstants.INTERACT_FEED_BLOG_ROUTING,
                    MqConstants.INTERACT_FEED_VOUCHER_ROUTING,
            }
    ))
    public void handleSendNormalToFollowers(FeedEventMessage feedEventMessage){
        log.info("推送数据是：{}为"+feedEventMessage);
        executorService.execute(() -> {
            followService.pushToFollowers(feedEventMessage);
        });
    }
}
