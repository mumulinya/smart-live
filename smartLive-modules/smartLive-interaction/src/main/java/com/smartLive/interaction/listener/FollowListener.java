package com.smartLive.interaction.listener;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.user.api.domain.BlogDTO;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class FollowListener {

    @Autowired
    private IFollowService followService;
    //博客推送
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = MqConstants.BLOG_FEED_QUEUE,declare = "true"),
            exchange = @Exchange(name = MqConstants.BLOG_EXCHANGE_NAME),
            key = MqConstants.BLOG_FEED_ROUTING
    ))
    public void handleSendBlogToFollowers(BlogDTO blogDTO ){
        followService.sendBlogToFollowers(blogDTO);
    }
}
