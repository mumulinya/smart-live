package com.smartLive.ai.service.rag;
import com.smartLive.interaction.api.DTO.CommentDTO;
import java.util.List;

public interface ICommentRagService {
    /**
     * 获取评论列表
     *
     * @param commentVO
     * @param userMessage
     * @return
     */
    List<CommentDTO> getComments(CommentDTO commentVO, String userMessage);
}
