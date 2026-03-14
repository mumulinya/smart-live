package com.smartLive.ai.strategy.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.AiMerchantMessage;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.support.MerchantAiPromptConstants;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.DTO.ShopTypeDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
public abstract class AbstractMerchantAiStrategy {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    protected static final String DEFAULT_INSTRUCTION = "No additional requirement";
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ChatClient merchantStrategyChatClient;
    protected final IAiMerchantSessionService merchantSessionService;
    protected final IAiMerchantMessageService merchantMessageService;
    protected final MerchantMessageChatMemoryManager memoryManager;
    protected final RemoteShopService remoteShopService;
    protected final IShopRagService shopRagService;
    private final ObjectMapper objectMapper;

    protected AbstractMerchantAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                                         IAiMerchantSessionService merchantSessionService,
                                         IAiMerchantMessageService merchantMessageService,
                                         MerchantMessageChatMemoryManager memoryManager,
                                         RemoteShopService remoteShopService,
                                         IShopRagService shopRagService,
                                         ObjectMapper objectMapper) {
        this.merchantStrategyChatClient = merchantStrategyChatClient;
        this.merchantSessionService = merchantSessionService;
        this.merchantMessageService = merchantMessageService;
        this.memoryManager = memoryManager;
        this.remoteShopService = remoteShopService;
        this.shopRagService = shopRagService;
        this.objectMapper = objectMapper;
    }

    public final Flux<String> execute(Long userId, MerchantChatDTO dto) {
        validateInput(userId, dto);
        AiMerchantSession session = merchantSessionService.getAndCheckSession(userId, dto.getSessionId());
        validateSession(dto, session);
        validateSceneInput(dto, session);

        String conversationId = buildConversationId(dto.getSessionId());
        memoryManager.rebuildConversationMemory(conversationId, dto.getSessionId());

        AiMerchantMessage userRecord = merchantMessageService.saveMessage(
                dto.getSessionId(),
                "user",
                resolveRawUserMessage(dto),
                dto.getReviewId(),
                dto.getProductId(),
                resolveMessageTimeRange(dto)
        );
        memoryManager.appendMessageToCache(userRecord);
        touchSession(dto.getSessionId());

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(MerchantAiPromptConstants.COMMON_SYSTEM_PROMPT),
                new SystemMessage(buildScenePrompt(dto, session)),
                new UserMessage(resolveInstruction(dto))
        ));
        StringBuilder aiReply = new StringBuilder();

        return merchantStrategyChatClient.prompt(prompt)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        aiReply.append(chunk);
                    }
                })
                .doFinally(signalType -> saveAssistantMessage(dto, aiReply.toString()))
                .doOnError(ex -> log.error("Merchant AI chat failed, sessionId={}", dto.getSessionId(), ex));
    }

    protected abstract String buildScenePrompt(MerchantChatDTO dto, AiMerchantSession session);

    protected void validateSceneInput(MerchantChatDTO dto, AiMerchantSession session) {
    }

    protected final String resolveInstruction(MerchantChatDTO dto) {
        String value = firstNonBlank(dto.getInstruction(), dto.getRawMessage(), dto.getMessage());
        return StringUtils.hasText(value) ? value.trim() : DEFAULT_INSTRUCTION;
    }

    protected final String normalizeDateRange(String dateRange) {
        if (!StringUtils.hasText(dateRange)) {
            return "week";
        }
        String normalized = dateRange.trim().toLowerCase(Locale.ROOT);
        if ("month".equals(normalized) || "quarter".equals(normalized)) {
            return normalized;
        }
        return "week";
    }

    protected final String formatDateRangeLabel(String dateRange) {
        return switch (normalizeDateRange(dateRange)) {
            case "month" -> "month";
            case "quarter" -> "quarter";
            default -> "week";
        };
    }

    protected final String formatDate(Date date) {
        return date == null ? "No data" : DATE_TIME_FORMAT.format(date);
    }

    protected final String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "No data";
    }

    protected final String defaultNumber(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    protected final String defaultLongNumber(Long value) {
        return value == null ? "0" : String.valueOf(value);
    }

    protected final String defaultDecimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    protected final <T> T convertAjaxData(AjaxResult ajaxResult, Class<T> targetClass, T defaultValue) {
        if (ajaxResult == null || !ajaxResult.isSuccess()) {
            return defaultValue;
        }
        Object data = ajaxResult.get(AjaxResult.DATA_TAG);
        if (data == null) {
            return defaultValue;
        }
        try {
            return objectMapper.convertValue(data, targetClass);
        } catch (IllegalArgumentException ex) {
            log.warn("Convert AjaxResult data to {} failed: {}", targetClass.getSimpleName(), ex.getMessage());
            return defaultValue;
        }
    }

    protected final ShopPromptContext getShopPromptContext(Long shopId) {
        ShopDTO shopDTO = remoteShopService.getShopById(shopId);
        ShopVO query = new ShopVO();
        query.setId(shopId);

        ShopVO ragShop = null;
        try {
            ragShop = shopRagService.getShopDetails(query, "shop basic information");
        } catch (Exception ex) {
            log.warn("Load shop info from vector store failed, shopId={}, error={}", shopId, ex.getMessage());
        }

        String shopName = ragShop != null && StringUtils.hasText(ragShop.getName())
                ? ragShop.getName()
                : shopDTO != null ? shopDTO.getName() : null;
        Long typeId = ragShop != null && ragShop.getTypeId() != null
                ? ragShop.getTypeId()
                : shopDTO != null ? shopDTO.getTypeId() : null;
        String shopType = resolveShopTypeName(typeId);
        return new ShopPromptContext(shopId, defaultText(shopName), defaultText(shopType), shopDTO, ragShop);
    }

    protected final String resolveRawUserMessage(MerchantChatDTO dto) {
        String value = firstNonBlank(dto.getInstruction(), dto.getRawMessage(), dto.getMessage());
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return switch (dto.getType().trim().toLowerCase(Locale.ROOT)) {
            case "reply" -> "generate review reply";
            case "analysis" -> "generate business analysis";
            case "copywrite" -> "generate product copywrite";
            case "suggest" -> "generate operation suggestion";
            default -> "merchant ai request";
        };
    }

    protected final String resolveMessageTimeRange(MerchantChatDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getType())) {
            return null;
        }
        String normalizedType = dto.getType().trim().toLowerCase(Locale.ROOT);
        if (!"analysis".equals(normalizedType)) {
            return null;
        }
        return normalizeDateRange(dto.getDateRange());
    }

    protected final String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void saveAssistantMessage(MerchantChatDTO dto, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        AiMerchantMessage assistantRecord = merchantMessageService.saveMessage(
                dto.getSessionId(),
                "assistant",
                content,
                dto.getReviewId(),
                dto.getProductId(),
                resolveMessageTimeRange(dto)
        );
        memoryManager.appendMessageToCache(assistantRecord);
        touchSession(dto.getSessionId());
    }

    private void touchSession(Long sessionId) {
        AiMerchantSession session = new AiMerchantSession();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        merchantSessionService.updateById(session);
    }

    private void validateInput(Long userId, MerchantChatDTO dto) {
        if (userId == null) {
            throw new ServiceException("User not logged in");
        }
        if (dto == null || dto.getSessionId() == null || dto.getShopId() == null || !StringUtils.hasText(dto.getType())) {
            throw new ServiceException("Invalid request");
        }
    }

    private void validateSession(MerchantChatDTO dto, AiMerchantSession session) {
        if (!dto.getShopId().equals(session.getShopId())) {
            throw new ServiceException("Session and shop mismatch");
        }
        if (!StringUtils.hasText(session.getType()) || !session.getType().equalsIgnoreCase(dto.getType().trim())) {
            throw new ServiceException("Session and type mismatch");
        }
    }

    private String buildConversationId(Long sessionId) {
        return "merchant::session::" + sessionId;
    }

    private String resolveShopTypeName(Long typeId) {
        if (typeId == null) {
            return null;
        }
        List<ShopTypeDTO> shopTypes = remoteShopService.getShopTypeList();
        if (shopTypes == null) {
            return null;
        }
        for (ShopTypeDTO shopType : shopTypes) {
            if (shopType != null && typeId.equals(shopType.getId()) && StringUtils.hasText(shopType.getName())) {
                return shopType.getName();
            }
        }
        return null;
    }

    @Getter
    protected static final class ShopPromptContext {

        private final Long shopId;
        private final String shopName;
        private final String shopType;
        private final ShopDTO shopDTO;
        private final ShopVO ragShop;

        private ShopPromptContext(Long shopId, String shopName, String shopType, ShopDTO shopDTO, ShopVO ragShop) {
            this.shopId = shopId;
            this.shopName = shopName;
            this.shopType = shopType;
            this.shopDTO = shopDTO;
            this.ragShop = ragShop;
        }
    }
}