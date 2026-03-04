package com.smartLive.interaction.strategy.resource;

import java.util.HashMap;
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
   /**
    * 获取资源
    */
   T getResourceById(Long sourceId);

   /**
    * 获取资源id
    * @param data
    * @return
    */
   Long getResourceId(T data);

   /**
    * 获取资源的作者的用户ID
    * @param data 资源对象
    * @return 作者的用户ID
    */
   default Long getAuthorId(T data) {
       return null;
   }
   /**
    * 获取资源内容
    */
   default HashMap<String,String> getResourceContentById(Long sourceId){
      return null;
   }
}
