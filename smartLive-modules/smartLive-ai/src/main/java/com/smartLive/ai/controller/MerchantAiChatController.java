package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.domain.DTO.MerchantChatRequestDTO;
import com.smartLive.ai.strategy.merchant.factory.AiStrategyFactory;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 商家 AI 聊天控制器。
 */
@RestController
@RequestMapping("/merchant")
public class MerchantAiChatController extends BaseController {

    @Autowired
    private AiStrategyFactory aiStrategyFactory;

    /**
     * 处理聊天字符串数据流。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody MerchantChatRequestDTO requestDTO) {
        Long userId = SecurityUtils.getUserId();
        MerchantChatDTO dto = requestDTO.toMerchantChatDTO();
        return aiStrategyFactory.getStrategy(dto.getType()).execute(userId, dto);
    }
}