package com.smartLive.blog.api.factory;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.smartLive.blog.api.RemoteBlogService;
@Component
public class RemoteBlogFallbackFactory implements FallbackFactory<RemoteBlogService> {

    @Override
    public RemoteBlogService create(Throwable cause) {
        return new RemoteBlogService() {
            @Override
            public R<Boolean> updateCommentById(Long blogId) {
                return R.fail("评论失败");
            }

            @Override
            public R<BlogDto> getBlogById(Long id) {
                return R.fail("查询博客信息失败");
            }

            @Override
            public R<Integer> getBlogCount(Long userId) {
                return R.fail("查询博客数量失败");
            }

            /**
             * 获取博客点赞数
             *
             * @param userId
             * @return
             */
            @Override
            public R<Integer> getLikeCount(Long userId) {

                return R.fail("查询点赞数失败");
            }
            /**
             * 获取博客总数
             *
             * @return
             */
            @Override
            public R<Integer> getBlogTotal() {
                return R.fail("查询博客总数失败");
            }
    };
 }
}
