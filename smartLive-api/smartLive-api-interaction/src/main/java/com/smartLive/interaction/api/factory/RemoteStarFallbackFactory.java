package com.smartLive.interaction.api.factory;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.dto.StarDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
public class RemoteStarFallbackFactory implements FallbackFactory<RemoteStarService> {
    @Override
    public RemoteStarService create(Throwable cause) {
        return new RemoteStarService() {
            @Override
            public R<Boolean> isStared(@RequestBody StarDTO starDTO){
                return R.fail("查询失败");
            }

            @Override
            public R<Integer> getStarCount(StarDTO starDTO) {
                return R.fail("获取失败");
            }

            @Override
            public R<Integer> getFanCount(StarDTO starDTO) {
                return R.fail("获取失败");
            }

            /**
             * 获取共同关注数
             *
             * @return
             */
            @Override
            public R<Integer> getCommonStarCount(StarDTO starDTO) {
                return R.fail("获取失败");
            }

            /**
             * 获取用户关注店铺数量
             *
             * @param
             * @return
             */
            @Override
            public R<Integer> getStarShopCount(StarDTO starDTO) {
                return R.fail("获取失败");
            }
        };

    }
}
