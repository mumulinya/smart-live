package com.smartLive.interaction.strategy.follow;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.smartLive.interaction.api.dto.FeedEventDTO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public interface FollowBaseStrategy<T> {

     /**
      * 策略标识 (USER / SHOP)
      */
     String getType();
     /**
      * 获取关注列表
      */
     List<SocialInfoVO> getFollowList(List<Long> sourceIdList);
}
