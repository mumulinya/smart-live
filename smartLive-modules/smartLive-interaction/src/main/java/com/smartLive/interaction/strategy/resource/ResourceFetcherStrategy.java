package com.smartLive.interaction.strategy.resource;

import com.smartLive.interaction.domain.vo.ResourceVO;
import java.util.List;
public interface ResourceFetcherStrategy {
   /**
    * 策略标识 (USER / SHOP)
    */
   String getType();

   /**
    * 获取资源列表
    */
   List<ResourceVO> getResourceList(List<Long> sourceIdList);


}
