package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.strategy.merchant.factory.AiStrategyFactory;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/merchant")
public class MerchantAiChatController extends BaseController {

    @Autowired
    private AiStrategyFactory aiStrategyFactory;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody MerchantChatDTO dto) {
        Long userId = SecurityUtils.getUserId();
        return aiStrategyFactory.getStrategy(dto.getType()).execute(userId, dto);
    }
}
