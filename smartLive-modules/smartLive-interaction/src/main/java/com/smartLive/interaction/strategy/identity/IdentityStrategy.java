package com.smartLive.interaction.strategy.identity;

import java.util.List;
/**
 * 获取信息策略
 */
public interface IdentityStrategy<T> {

     /**
      * 策略标识 (USER / SHOP)
      */
     Integer getType();
     /**
      * 获取关注列表
      */
     List<T> getFollowList(List<Long> sourceIdList);
}
