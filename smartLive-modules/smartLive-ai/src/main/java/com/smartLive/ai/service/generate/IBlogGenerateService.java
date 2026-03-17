package com.smartLive.ai.service.generate;

import com.smartLive.ai.domain.DTO.BlogGenerateDTO;
import com.smartLive.ai.entity.vo.BlogGenerateVO;

/**
 * 博客内容生成服务接口。
 *
 * 作者：smartLive
 */
public interface IBlogGenerateService {

    /**
     * 根据选择的店铺、用户描述和博客风格，使用 AI 生成带标题和正文的博客信息
     *
     * @param dto 生成参数
     * @return 博客标题及正文
     */
    BlogGenerateVO generate(BlogGenerateDTO dto);
}
