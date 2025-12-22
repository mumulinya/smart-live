package com.smartLive.interaction.strategy.identity;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 获取信息策略
 */
@Component
public interface IdentityStrategy<T> {

     /**
      * 策略标识 (USER / SHOP)
      */
     String getType();
     /**
      * 获取关注列表
      */
     List<SocialInfoVO> getFollowList(List<Long> sourceIdList);
}
