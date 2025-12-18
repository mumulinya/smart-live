package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Collection;

import java.util.List;


/**
 * 关注Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ICollectionShopService extends IService<Collection>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    public Collection selectCollectionShopById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
    public List<Collection> selectCollectionShopList(Collection follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int insertCollectionShop(Collection follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int updateCollectionShop(Collection follow);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键集合
     * @return 结果
     */
    public int deleteCollectionShopByIds(Long[] ids);

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    public int deleteCollectionShopById(Long id);

    /**
     * 关注或取关
     * @param shopId
     * @param isCollectionShop
     * @return
     */
    Result follow(Long shopId, Boolean isCollectionShop);

    /**
     * 判断是否关注
     * @param followUserId
     * @return
     */
    Result isCollection(Long followUserId);

    /**
     * 获取粉丝列表
     * @return
     */
    Result getFans(Long followUserId);

    /**
     * 获取店铺列表
     * @return
     */
    Result getCollectionShops(Long  userId,Integer current);

    /**
     * 获取收藏数量
     * @param userId
     * @return
     */
    Integer getCollectCount(Long userId);
}
