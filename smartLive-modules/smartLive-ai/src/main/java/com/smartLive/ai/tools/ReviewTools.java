package com.smartLive.ai.tools;

import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewTools {

    public final IReviewRagService reviewRagService;

    @Tool(name = "getReviews", description = "根据条件获取评价列表")
    public List<ReviewDTO> getReviews(
            @ToolParam(description = "评价来源类型")
            int sourceType,
            @ToolParam(description = "评价来源名称")
            String sourceName,
            @ToolParam(description = "评价来源ID")
            Long sourceId,
            @ToolParam(description = "用户原始消息，用于RAG查询")
            String userMessage
    ) {
        log.info("获取评价列表，参数：{},{},{},{}", sourceType, sourceId, sourceName, userMessage);
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setSourceType(sourceType);
        reviewDTO.setSourceId(sourceId);
        reviewDTO.setSourceName(sourceName);
        List<ReviewDTO> reviewList = reviewRagService.getReviews(reviewDTO, userMessage);
        return reviewList;
    }
}
