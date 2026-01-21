package com.smartLive.interaction.api.factory;
import com.smartLive.interaction.api.RemoteCommentService;
import com.smartLive.interaction.api.DTO.CommentDTO;
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
             * @param commentDTO
             * @return
             */
            @Override
            public Integer getCommentCount(CommentDTO commentDTO) {
                log.error("查询评论数失败", cause.getMessage());
                return 0;
            }
            /**
             * 获取评论总数
             * @return
             */
            @Override
            public Integer getCommentTotal() {
                log.error("查询评论总数失败", cause.getMessage());
                return 0;
            }

            @Override
            public List<CommentDTO> searchCommentList() {
                log.error("查询评论列表失败", cause.getMessage());
                return null;
            }

            @Override
            public void saveAiCreateComment(List<CommentDTO> comments) {
                log.error("保存AI创建的评论失败", cause.getMessage());
            }
        };
    }
}
