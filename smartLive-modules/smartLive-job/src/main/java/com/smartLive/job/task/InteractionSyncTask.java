package com.smartLive.job.task;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.InteractionSyncTriggerMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("interactionSyncTask")
@Slf4j
public class InteractionSyncTask
{
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void triggerSync()
    {
        try
        {
            InteractionSyncTriggerMessage message = InteractionSyncTriggerMessage.builder()
                    .source("job:interactionSyncTask.triggerSync")
                    .triggerTime(System.currentTimeMillis())
                    .build();
            MqMessageSendUtils.sendMqMessage(
                    rabbitTemplate,
                    MqConstants.INTERACTION_SYNC_EXCHANGE_NAME,
                    MqConstants.INTERACTION_SYNC_ROUTING,
                    message
            );
            log.info("interaction sync trigger message sent, source={}", message.getSource());
        }
        catch (Exception e)
        {
            log.error("send interaction sync trigger message failed", e);
            throw new RuntimeException("trigger interaction sync by mq failed", e);
        }
    }
}
