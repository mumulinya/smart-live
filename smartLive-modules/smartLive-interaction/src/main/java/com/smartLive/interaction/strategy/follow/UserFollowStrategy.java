package com.smartLive.interaction.strategy.follow;

import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class UserFollowStrategy implements FollowStrategy {

    private final RemoteAppUserService remoteAppUserService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.USER_RESOURCE.getCode();
    }


    @Override
    public void transFollowCountFromRedis2DB(Map<Long, Integer> updateMap) {
        remoteAppUserService.updateFolloweeCountBatch(updateMap);
    }

    @Override
    public void transFansCountFromRedis2DB(Map<Long, Integer> updateMap) {
        remoteAppUserService.updateFansCountBatch(updateMap);
    }

    @Override
    public Integer getStarCount(Long sourceId) {
        return 0;
    }

    /**
     * 从用户表获取粉丝数
     *
     * @param sourceId 用户 ID
     * @return 粉丝数
     */
    @Override
    public Integer getFanCount(Long sourceId) {
        UserDTO user = remoteAppUserService.queryUserById(sourceId);
        return user != null ? user.getFans() : null;
    }

    /**
     * 从用户表获取关注数
     *
     * @param userId 用户 ID
     * @return 关注数
     */
    @Override
    public Integer getFollowCount(Long userId) {
        UserDTO user = remoteAppUserService.queryUserById(userId);
        return user != null ? user.getFollowee() : null;
    }
}
