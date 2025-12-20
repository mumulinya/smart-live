package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Collection;
import com.smartLive.interaction.domain.Comment;

import java.util.List;


/**
 * 关注Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ICollectionService extends IService<Collection>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
     Collection selectCollectionShopById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
     List<Collection> selectCollectionShopList(Collection follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int insertCollectionShop(Collection follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int updateCollectionShop(Collection follow);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键集合
     * @return 结果
     */
     int deleteCollectionShopByIds(Long[] ids);

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
     int deleteCollectionShopById(Long id);

    /**
     * 关注或取关
      * @param collection
     * @return
     */
    Result collection(Collection collection);

    /**
     * 判断是否收藏
     * @param
     * @return
     */
    Result isCollection(Collection collection);

    /**
     * 获取店铺列表
     * @return
     */
    Result getCollectionList(Collection collection,Integer current);

    /**
     * 获取收藏数量
     * @param collection
     * @return
     */
    Integer getCollectCount(Collection collection);
}
