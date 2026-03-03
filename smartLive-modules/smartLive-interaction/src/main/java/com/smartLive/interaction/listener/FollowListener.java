package com.smartLive.interaction.listener;

import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.interaction.service.IFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FollowListener {

    @Autowired
    private IFollowService followService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = InteractionMqConstants.INTERACT_FEED_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = InteractionMqConstants.INTERACTION_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = InteractionMqConstants.INTERACTION_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = InteractionMqConstants.INTERACT_FEED_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = InteractionMqConstants.INTERACT_FEED_ROUTING
    ))
    public void handleSendNormalToFollowers(FeedEventMessage feedEventMessage) {
        log.info("follow feed message: {}", feedEventMessage);
        followService.pushToFollowers(feedEventMessage);
    }

    /**
     * Dead-letter queue listener
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = InteractionMqConstants.INTERACTION_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = InteractionMqConstants.INTERACTION_DEAD_LETTER_EXCHANGE_NAME, type = ExchangeTypes.DIRECT),
            key = InteractionMqConstants.INTERACTION_DEAD_LETTER_ROUTING
    ))
    public void handleInteractionDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("interaction dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}