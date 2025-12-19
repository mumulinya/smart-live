package com.smartLive.interaction.listener;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.interaction.api.dto.FeedEventDTO;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.interaction.strategy.follow.FollowBaseStrategy;
import com.smartLive.user.api.domain.BlogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
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
            value = @Queue(name = MqConstants.INTERACT_FEED_NORMAL_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.INTERACT_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = {
                    MqConstants.INTERACT_FEED_BLOG_ROUTING,
                    MqConstants.INTERACT_FEED_VOUCHER_ROUTING,
            }
    ))
    public void handleSendNormalToFollowers(FeedEventDTO feedEventDTO){
        executorService.execute(() -> {
            followService.pushToFollowers(feedEventDTO);
        });
    }
    //紧急数据推送
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.INTERACT_FEED_URGENT_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.INTERACT_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = {
                    MqConstants.INTERACT_FEED_BLOG_ROUTING,
                    MqConstants.INTERACT_FEED_VOUCHER_ROUTING,
            }
    ))
    public void handleSendUrgentToFollowers(FeedEventDTO feedEventDTO){
        executorService.execute(() -> {
            followService.pushToFollowers(feedEventDTO);
        });
    }
}
