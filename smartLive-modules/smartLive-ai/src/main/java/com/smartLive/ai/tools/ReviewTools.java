package com.smartLive.ai.tools;

import com.smartLive.ai.service.rag.IReviewRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 评价工具集。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewTools {

    private final IReviewRagService reviewRagService;

    @Tool(description = "Get a review summary for a shop, blog or product resource.")
    public String getReviewSummary(
            @ToolParam(description = "Resource type: 2=shop, 3=blog, 4=product.", required = true)
            Integer sourceType,
            @ToolParam(description = "Resource id.", required = false)
            Long sourceId,
            @ToolParam(description = "Minimum score.", required = false)
            Integer minScore,
            @ToolParam(description = "Maximum score.", required = false)
            Integer maxScore,
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            ToolContext toolContext
    ) {
        log.info("Calling getReviewSummary | sourceType={}, sourceId={}, minScore={}, maxScore={}",
                sourceType, sourceId, minScore, maxScore);
        return reviewRagService.getReviewSummary(sourceType, sourceId, minScore, maxScore, userMessage);
    }
}
