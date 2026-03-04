package com.smartLive.interaction.strategy.like;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户维度点赞数同步策略：同步所有被点赞数据（博客、评价等产生的点赞，累加到作者的用户实体上）
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserLikeStrategy implements LikeStrategy {

    private final RemoteAppUserService remoteUserService;

    @Override
    public Integer getType() {
        // 返回用户的 GlobalBizTypeEnum.USER 的 Code
        return com.smartLive.common.core.enums.GlobalBizTypeEnum.USER.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用用户服务，同步用户点赞总数数据");
        Boolean b = remoteUserService.updateUserLikedBatch(updateMap);
        if (b) {
            log.info("同步用户点赞总数成功");
        } else {
            log.warn("同步用户点赞总数失败");
        }
    }

    /**
     * 获取点赞数
     *
     * @param sourceId 用户ID
     * @return 用户的总点赞数
     */
    @Override
    public Integer getLikeCount(Long sourceId) {
        return remoteUserService.getUserLikedCount(sourceId);
    }
}
