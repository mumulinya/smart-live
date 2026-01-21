package com.smartLive.interaction.api.factory;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@Slf4j
public class RemoteFollowFallbackFactory implements FallbackFactory<RemoteFollowService> {
    @Override
    public RemoteFollowService create(Throwable cause) {
        return new RemoteFollowService() {
            @Override
            public Boolean isFollowed(@RequestBody FollowDTO followDTO){
                log.error("查询是否关注失败:{}",cause.getMessage());
                return false;
            }

            @Override
            public Integer getFollowCount(FollowDTO followDTO) {
                log.error("获取关注数失败:{}",cause.getMessage());
                return 0;
            }

            @Override
            public Integer getFanCount(FollowDTO followDTO) {
                log.error("获取粉丝数失败:{}",cause.getMessage());
                return 0;
            }

            /**
             * 获取共同关注数
             *
             * @return
             */
            @Override
            public Integer getCommonFollowCount(FollowDTO followDTO) {
                log.error("获取共同关注数失败:{}",cause.getMessage());
                return 0;
            }
        };

    }
}
