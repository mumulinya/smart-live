package com.smartLive.blog.listener;

import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BlogListener {

    @Autowired
    private IBlogService blogService;
    //评论监听器
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.BLOG_COMMENT_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.BLOG_EXCHANGE_NAME),
            key = MqConstants.BLOG_COMMENT_ROUTING
    ))
    public void handleUpdateCommentCount(Long blogId ){
        log.info("评论监听器收到消息：{}",blogId);
        blogService.updateCommentById(blogId);
    }
}
