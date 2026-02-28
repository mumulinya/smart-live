package com.smartLive.user.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.user.domain.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


/**
 * 用户Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface UserInfoMapper extends BaseMapper<UserInfo>
{
    /**
     * 查询用户
     * 
     * @param userId 用户主键
     * @return 用户
     */
    public UserInfo selectUserInfoByUserId(Long userId);

    /**
     * 查询用户列表
     * 
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

    /**
     * 新增用户
     * 
     * @param userInfo 用户
     * @return 结果
     */
    public int insertUserInfo(UserInfo userInfo);

    /**
     * 修改用户
     * 
     * @param userInfo 用户
     * @return 结果
     */
    public int updateUserInfo(UserInfo userInfo);

    /**
     * 删除用户
     * 
     * @param userId 用户主键
     * @return 结果
     */
    public int deleteUserInfoByUserId(Long userId);

    /**
     * 批量删除用户
     * 
     * @param userIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserInfoByUserIds(Long[] userIds);

    /**
     * Batch update user fans count.
     */
    @Update("<script>" +
            "UPDATE user_info " +
            "SET fans = CASE user_id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE user_id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateFansCountBatch(@Param("map") Map<Long, Integer> updateMap);

    /**
     * Batch update user followee count.
     */
    @Update("<script>" +
            "UPDATE user_info " +
            "SET followee = CASE user_id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE user_id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateFolloweeCountBatch(@Param("map") Map<Long, Integer> updateMap);
}
