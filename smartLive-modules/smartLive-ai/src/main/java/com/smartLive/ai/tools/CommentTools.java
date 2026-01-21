package com.smartLive.ai.tools;

import com.smartLive.ai.service.rag.ICommentRagService;
import com.smartLive.interaction.api.DTO.CommentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentTools {

    public final ICommentRagService commentRagService;
    @Tool(name = "getComments" ,description = "根据条件获取评论列表")
    public List<CommentDTO>getComments(
            @ToolParam(description = "评论来源类型")
            int sourceType,
            @ToolParam(description = "评论来源名称")
            String sourceName,
            @ToolParam(description = "评论来源ID")
            Long sourceId,
            @ToolParam(description = "用户原始消息，用于RAG查询")
            String userMessage
    ) {
        log.info("获取评论列表，参数：{},{},{},{}", sourceType, sourceId, sourceName, userMessage);
        CommentDTO commentVO=new CommentDTO();
        commentVO.setSourceType(sourceType);
        commentVO.setSourceId(sourceId);
        commentVO.setSourceName(sourceName);
        List<CommentDTO> commentList=commentRagService.getComments(commentVO,userMessage);
        return commentList;
    }
}
