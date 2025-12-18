package com.smartLive.interaction.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.interaction.domain.Follow;


/**
 * 关注Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface FollowMapper extends BaseMapper<Follow>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    public Follow selectFollowUserById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
     List<Follow> selectFollowUserList(Follow follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int insertFollowUser(Follow follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int updateFollowUser(Follow follow);

    /**
     * 删除关注
     * 
     * @param id 关注主键
     * @return 结果
     */
     int deleteFollowUserById(Long id);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
     int deleteFollowUserByIds(Long[] ids);
}
