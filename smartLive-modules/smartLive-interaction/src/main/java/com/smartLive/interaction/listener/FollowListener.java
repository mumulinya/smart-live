package com.smartLive.interaction.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.service.IFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class FollowListener {

    @Autowired
    private IFollowService followService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听关注 Feed 流投递队列，处理大V发动态后推送给粉丝的信箱
     */
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
    public void handleSendNormalToFollowers(
            FeedEventMessage feedEventMessage,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {

        if (feedEventMessage == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] Feed message missing messageId, reject consume");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String idempotentKey = RedisMqIdempotentConstants.FOLLOW_PREFIX + "messageId:" + messageId;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] Duplicate message skipped, key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            followService.pushToFollowers(feedEventMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] Feed consume failed, clear idempotent key and trigger retry, key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("Feed message processing failed, trigger local retry", e);
        }
    }

    /**
     * 监听因为信箱推送等互动异常导致的死信队列
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
