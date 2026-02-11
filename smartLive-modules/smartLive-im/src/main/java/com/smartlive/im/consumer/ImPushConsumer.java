package com.smartlive.im.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.MqConstants;
import com.smartlive.im.handler.NettyChatHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ImPushConsumer {

    @Autowired
    private NettyChatHandler nettyChatHandler;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 监听推送消息
     * 路由键: im.push.user
     * 数据格式: { "userId": 123, "json": "{...}" }
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "im.push.queue"),
            exchange = @Exchange(name = MqConstants.CHAT_EXCHANGE_NAME, type = "topic"),
            key = "im.push.user"
    ))
    public void handlePushMessage(Map<String, Object> map) {
        try {
            Long userId = Long.valueOf(map.get("userId").toString());
            String json = (String) map.get("json");

            if (NettyChatHandler.isUserOnline(userId)) {
                NettyChatHandler.pushMessageToUser(userId, json);
                log.info("MQ推送消息给用户: {}", userId);
            }
        } catch (Exception e) {
            log.error("MQ推送消息处理失败, 消息内容: {}", map, e);
        }
    }
}
