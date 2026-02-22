package com.smartLive.interaction.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.InteractionSyncTriggerMessage;
import com.smartLive.interaction.service.ISyncDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InteractionSyncListener
{
    @Autowired
    private ISyncDataService syncDataService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.INTERACTION_SYNC_QUEUE, declare = "true"),
            exchange = @Exchange(name = MqConstants.INTERACTION_SYNC_EXCHANGE_NAME, type = ExchangeTypes.DIRECT),
            key = MqConstants.INTERACTION_SYNC_ROUTING
    ))
    public void handleTrigger(InteractionSyncTriggerMessage message)
    {
        log.info("receive interaction sync trigger message, source={}, triggerTime={}",
                message == null ? null : message.getSource(),
                message == null ? null : message.getTriggerTime());
        syncDataService.syncAllData();
    }
}
