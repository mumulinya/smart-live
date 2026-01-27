package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.DTO.LikeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoteLikeFallbackFactory implements FallbackFactory<RemoteLikeService> {
    @Override
    public RemoteLikeService create(Throwable cause) {
        return new RemoteLikeService() {
            @Override
            public Boolean isLike(LikeDTO like) {
                log.error("查询是否点赞失败:{}", cause.getMessage());
                return false;
            }

            /**
             * 获取点赞数
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public Integer getLikeCount(LikeDTO LikeDTO) {
                log.error("查询点赞数失败:{}", cause.getMessage());
                return 0;
            }
            /**
             * 获取共同关注数
             *
             * @param LikeDTO
             * @return
             */
            @Override
            public Integer getCommonLikeCount(LikeDTO LikeDTO) {
                log.error("查询共同点赞数失败:{}", cause.getMessage());
                return 0;
            }

            /**
             * 获取用户点赞数
             *
             * @param likeDTO
             * @return
             */
            @Override
            public Integer getUserLikeCount(LikeDTO likeDTO) {
                log.error("查询用户点赞数失败:{}", cause.getMessage());
                return 0;
            }
        };
    }
}
