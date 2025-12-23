package com.smartLive.interaction.strategy.resource;

import com.smartLive.interaction.domain.vo.ResourceVO;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 资源获取策略
 */
public interface ResourceStrategy {
   /**
    * 策略标识 (USER / SHOP)
    */
   Integer getType();

   /**
    * 获取资源列表
    */
   List<ResourceVO> getResourceList(List<Long> sourceIdList);


}
