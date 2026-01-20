package com.smartLive.interaction.service.impl;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.service.IFeedService;
import com.smartLive.interaction.strategy.feed.FeedStrategy;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.marketing.api.dto.VoucherDTO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class feedServiceImpl implements IFeedService {
    @Autowired
    RedisService redisService;
    @Autowired
    RemoteBlogService remoteBlogService;
    @Autowired
    RemoteVoucherService remoteVoucherService;
    @Autowired
    private Map<Integer, FeedStrategy> feedStrategyMap;
    @Override
    public ScrollResult queryFeedList(Integer feedType, Long max, Integer offset) {
        if(UserContextHolder.getUser() == null){
            return new ScrollResult();
        }
        //获取当前登录用户
        Long userId = UserContextHolder.getUser().getId();
        return feedStrategyMap.get(feedType).queryPage(userId, max, offset);
    }
}
