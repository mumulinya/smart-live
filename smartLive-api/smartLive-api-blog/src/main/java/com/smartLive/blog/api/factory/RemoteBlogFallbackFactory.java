package com.smartLive.blog.api.factory;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.smartLive.blog.api.RemoteBlogService;

import java.util.List;
import java.util.Map;

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

            /**
             * 获取博客列表
             *
             * @param sourceIdList
             */
            @Override
            public R<List<BlogDto>> getBlogListByIds(List<Long> sourceIdList) {
                return R.fail("查询博客列表失败");
            }

            /**
             * 批量更新点赞数
             *
             * @param updateMap
             */
            @Override
            public R<Boolean> updateLikeCountBatch(Map<Long, Integer> updateMap) {
                return R.fail("批量更新点赞数失败");
            }

            /**
             * 批量更新评论数
             *
             * @param updateMap
             */
            @Override
            public R<Boolean> updateCommentCountBatch(Map<Long, Integer> updateMap) {
                return R.fail("批量更新评论数失败");
            }
        };
 }
}
