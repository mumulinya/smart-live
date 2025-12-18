package com.smartLive.interaction.api.factory;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.RemoteFollowService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteFollowFallbackFactory implements FallbackFactory<RemoteFollowService> {
    @Override
    public RemoteFollowService create(Throwable cause) {
        return new RemoteFollowService() {
            @Override
            public Result isFollowed(Long followUserId){
                return Result.fail("查询失败");
            }

            @Override
            public Result getFollowCount(Long followUserId) {
                return Result.fail("获取失败");
            }

            @Override
            public Result getFanCount(Long followUserId) {
                return Result.fail("获取失败");
            }

            /**
             * 获取共同关注数
             *
             * @param userId        用户id, currentUserId 当前用户id
             * @param currentUserId
             * @return
             */
            @Override
            public Result getCommonFollowCount(Long userId, Long currentUserId) {
                return Result.fail("获取失败");
            }

            /**
             * 获取用户关注店铺数量
             *
             * @param userId 用户id
             * @return
             */
            @Override
            public Result getFollowShopCount(Long userId) {
                return Result.fail("获取失败");
            }
        };

    }
}
