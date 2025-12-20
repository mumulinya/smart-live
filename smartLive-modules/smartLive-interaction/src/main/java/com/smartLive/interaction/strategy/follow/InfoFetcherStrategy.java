package com.smartLive.interaction.strategy.follow;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public interface InfoFetcherStrategy<T> {

     /**
      * 策略标识 (USER / SHOP)
      */
     String getType();
     /**
      * 获取关注列表
      */
     List<SocialInfoVO> getFollowList(List<Long> sourceIdList);
}
