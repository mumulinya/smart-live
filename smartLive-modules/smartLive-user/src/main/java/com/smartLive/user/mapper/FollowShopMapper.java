package com.smartLive.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.user.domain.Follow;
import com.smartLive.user.domain.FollowShop;

import java.util.List;

/**
 * 关注Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface FollowShopMapper extends BaseMapper<FollowShop>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    public Follow selectFollowById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
    public List<Follow> selectFollowList(Follow follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int insertFollow(Follow follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int updateFollow(Follow follow);

    /**
     * 删除关注
     * 
     * @param id 关注主键
     * @return 结果
     */
    public int deleteFollowById(Long id);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFollowByIds(Long[] ids);
}
