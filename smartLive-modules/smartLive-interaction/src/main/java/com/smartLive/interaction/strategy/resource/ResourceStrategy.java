package com.smartLive.interaction.strategy.resource;

import java.util.List;
/**
 * 资源获取策略
 */
public interface ResourceStrategy<T> {
   /**
    * 策略标识 (USER / SHOP)
    */
   Integer getType();

   /**
    * 获取资源列表
    */
   List<T> getResourceList(List<Long> sourceIdList);

}
