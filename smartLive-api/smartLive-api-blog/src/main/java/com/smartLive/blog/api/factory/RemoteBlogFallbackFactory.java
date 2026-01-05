package com.smartLive.blog.api.factory;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.smartLive.blog.api.RemoteBlogService;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteBlogFallbackFactory implements FallbackFactory<RemoteBlogService> {

    @Override
    public RemoteBlogService create(Throwable cause) {
        return new RemoteBlogService() {

            @Override
            public BlogDto getBlogById(Long id) {
                log.error("查询博客失败:{}", cause.getMessage());
                return null;
            }

            @Override
            public Integer getBlogCount(Long userId) {
                log.error("查询博客数量失败:{}", cause.getMessage());
                return 0;
            }

            /**
             * 获取博客点赞数
             *
             * @param userId
             * @return
             */
            @Override
            public Integer getLikeCount(Long userId) {
                log.error("查询博客点赞数失败:{}", cause.getMessage());
                return 0;
            }
            /**
             * 获取博客总数
             *
             * @return
             */
            @Override
            public Integer getBlogTotal() {
                log.error("查询博客总数失败:{}", cause.getMessage());
                return 0;
            }

            /**
             * 获取博客列表
             *
             * @param sourceIdList
             */
            @Override
            public List<BlogDto> getBlogListByIds(List<Long> sourceIdList) {
                log.error("查询博客列表失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 批量更新点赞数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateLikeCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新点赞数失败:{}", cause.getMessage());
                return false;
            }

            /**
             * 批量更新评论数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateCommentCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新评论数失败:{}", cause.getMessage());
                return false;
            }
        };
 }
}
