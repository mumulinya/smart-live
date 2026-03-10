package com.smartLive.ai.service.generate;

import com.smartLive.ai.domain.DTO.ReviewGenerateDTO;

/**
 * AI Review Generate Service Interface
 *
 * @author smartLive
 */
public interface IReviewGenerateService {

    /**
     * 根据消费评分、用户体验及关联店铺信息，使用 AI 撰写评价正文
     *
     * @param dto 生成参数
     * @return 评价正文
     */
    String generate(ReviewGenerateDTO dto);
}
