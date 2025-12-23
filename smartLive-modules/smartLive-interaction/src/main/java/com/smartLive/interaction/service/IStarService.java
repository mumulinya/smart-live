package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Star;

import java.util.List;


/**
 * 关注Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IStarService extends IService<Star>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
     Star selectCollectionShopById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
     List<Star> selectCollectionShopList(Star follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int insertCollectionShop(Star follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int updateCollectionShop(Star follow);

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
      * @param star
     * @return
     */
    Result star(Star star);

    /**
     * 判断是否收藏
     * @param
     * @return
     */
    Result isStar(Star star);

    /**
     * 获取收藏列表
     * @return
     */
    Result getStarList(Star star, Integer current);

    /**
     * 获取收藏数量
     * @param star
     * @return
     */
    Integer getStarCount(Star star);
}
