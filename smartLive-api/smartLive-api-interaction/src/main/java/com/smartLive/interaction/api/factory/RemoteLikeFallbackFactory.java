package com.smartLive.interaction.api.factory;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.dto.LikeDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteLikeFallbackFactory implements FallbackFactory<RemoteLikeService> {
    @Override
    public RemoteLikeService create(Throwable cause) {
        return new RemoteLikeService() {
            @Override
            public R<Boolean> isLike(LikeDTO like) {
                return R.fail("查询失败");
            }

            /**
             * 获取关注数
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public R<Integer> getLikeCount(LikeDTO LikeDTO) {
                return  R.fail("查询失败");
            }

            /**
             * 获取粉丝数
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public R<Integer> getFanCount(LikeDTO LikeDTO) {
                return R.fail("查询失败");
            }

            /**
             * 获取共同关注数
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public R<Integer> getCommonLikeCount(LikeDTO LikeDTO) {
                return R.fail("查询失败");
            }

            /**
             * 获取用户关注店铺数量
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public R<Integer> getLikeShopCount(LikeDTO LikeDTO) {
                return R.fail("查询失败");
            }
        };
    }
}
