package com.smartLive.interaction.strategy.like;

import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户维度点赞数同步策略
 * 同步所有被点赞数据（博客、评价等产生的点赞，累加到作者的用户实体上）
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {

    private final RemoteAppUserService remoteUserService;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.USER.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        if (updateMap == null || updateMap.isEmpty()) {
            return;
        }

        try {
            Boolean result = remoteUserService.updateUserLikedBatch(updateMap);
            if (!result) {
                throw new RuntimeException("用户点赞数更新失败");
            }
        } catch (Exception e) {
            log.error("用户点赞数同步失败", e);
        }
    }

    @Override
    public Integer getLikeCount(Long sourceId) {
        if (sourceId == null || sourceId <= 0) {
            return 0;
        }

        try {
            return remoteUserService.getUserLikedCount(sourceId);
        } catch (Exception e) {
            log.error("获取用户点赞数异常: sourceId={}", sourceId, e);
            return 0;
        }
    }

    @Override
    protected Object getSourceData(Long sourceId) {
        return sourceId;
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.USER.getBizDomain();
    }

    @Override
    protected String getActionType() {
        return "user_liked_count";
    }
}
