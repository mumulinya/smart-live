package com.smartLive.user.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.service.IFollowService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserListener {

    @Autowired
    private IFollowService followService;
    //
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.BLOG_FEED_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.BLOG_EXCHANGE_NAME),
            key = MqConstants.BLOG_FEED_ROUTING
    ))
    public void handleSendBlogToFollowers(BlogDTO blogDTO ){
        followService.sendBlogToFollowers(blogDTO);
    }
}
