package com.smartLive.ai.strategy.merchant.impl;

import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("replyStrategy")
public class ReplyAiStrategy extends AbstractMerchantAiStrategy {

    public ReplyAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                           IAiMerchantSessionService merchantSessionService,
                           IAiMerchantMessageService merchantMessageService,
                           MerchantMessageChatMemoryManager memoryManager) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager);
    }

    @Override
    protected String getRolePrompt() {
        return "Generate a polite and sincere merchant reply suggestion for the provided customer review.";
    }
}
