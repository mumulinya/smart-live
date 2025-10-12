package com.smartLive.comment.api.factory;

import com.smartLive.comment.api.RemoteCommentService;
import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
@Component
public class RemoteCommentFallbackFactory implements FallbackFactory<RemoteCommentService> {
    @Override
    public RemoteCommentService create(Throwable cause) {
        return new RemoteCommentService() {
            /**
             * 获取评论数量
             * @param userId
             * @return
             */
            @Override
            public R<Integer> getCommentCount(Long userId) {
                return R.fail("查询评论数失败");
            }

        };

    }
}
