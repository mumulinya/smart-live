package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.rag.IReviewRagService;
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

    @Tool(description = "获取店铺或商品的用户评价总结，当用户询问评价、口碑、推荐、体验时调用")
    public String getReviewSummary(
            @ToolParam(description = "来源类型：1=店铺评价，2=文章评价，3=团购商品评价", required = true)
            Integer sourceType,
            @ToolParam(description = "来源ID，店铺ID或商品ID，获取不到传null", required = false)
            Long sourceId,
            @ToolParam(description = "最低评分过滤：用户问高评分传4或5，不限制传null", required = false)
            Integer minScore,
            @ToolParam(description = "最高评分过滤：用户问差评/低评分传2或3，不限制传null", required = false)
            Integer maxScore,
            @ToolParam(description = "用户原始问题，原样传入不要修改", required = false)
            String userMessage
    ) {
        log.info("📝 获取评价总结 | sourceType={}, sourceId={}, minScore={}, maxScore={}",
                sourceType, sourceId, minScore, maxScore);
        return reviewRagService.getReviewSummary(sourceType, sourceId, minScore, maxScore, userMessage);
    }
}