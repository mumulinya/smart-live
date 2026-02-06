package com.smartLive.ai.listener;

import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.strategy.handlers.CommentHandler;
import com.smartLive.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class AiListener {

    @Autowired
    private CommentHandler commentHandler;
    @Autowired
    private ExecutorService executorService;
    //
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.AI_COMMENT_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.AI_EXCHANGE_NAME),
            key = MqConstants.AI_COMMENT_ROUTING
    ))
    public void handleAiCreateComment(List<AIGenerateRequest> list){
      executorService.submit(()->{
          list.forEach(item->{
              item.setSourceIds(item.getSourceIds().subList(0, 4));
          });
          log.info("线程：{}接收到ai生成评论请求{}",list);
          commentHandler.aiCreateComment(list);
      });
    }
}
