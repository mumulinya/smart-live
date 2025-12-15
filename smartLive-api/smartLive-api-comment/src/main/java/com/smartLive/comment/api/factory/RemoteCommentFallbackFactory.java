package com.smartLive.comment.api.factory;

import com.smartLive.comment.api.RemoteCommentService;
import com.smartLive.comment.api.dto.CommentDTO;
import com.smartLive.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
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
            /**
             * 获取评论总数
             * @return
             */
            @Override
            public R<Integer> getCommentTotal() {
                return R.fail("查询评论总数失败");
            }

            @Override
            public List<CommentDTO> searchCommentList() {
                log.error("查询评论列表失败");
                return null;
            }

            @Override
            public void saveAiCreateComment(List<CommentDTO> comments) {
                log.error("保存AI创建的评论失败");
            }
        };
    }
}
