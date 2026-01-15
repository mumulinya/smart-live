package com.smartLive.interaction.api.factory;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.dto.StarDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@Slf4j
public class RemoteStarFallbackFactory implements FallbackFactory<RemoteStarService> {
    @Override
    public RemoteStarService create(Throwable cause) {
        return new RemoteStarService() {
            @Override
            public Boolean isStar(@RequestBody StarDTO starDTO){
                log.error("查询是否收藏失败:{}",cause.getMessage());
                return false;
            }

            @Override
            public Integer getStarCount(StarDTO starDTO) {
                log.error("获取收藏数失败:{}",cause.getMessage());
                return 0;
            }

            @Override
            public Integer getFanCount(StarDTO starDTO) {
                log.error("获取粉丝数失败:{}",cause.getMessage());
                return 0;
            }

            /**
             * 获取共同关注数
             *
             * @return
             */
            @Override
            public Integer getCommonStarCount(StarDTO starDTO) {
                log.error("获取共同关注数失败:{}",cause.getMessage());
                return 0;
            }

            /**
             * 获取用户关注店铺数量
             *
             * @param
             * @return
             */
            @Override
            public Integer getStarShopCount(StarDTO starDTO) {
                log.error("获取用户关注店铺数量失败:{}",cause.getMessage());
                return 0;
            }
        };

    }
}
