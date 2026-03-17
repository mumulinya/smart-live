package com.smartLive.ai.strategy.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.ai.service.merchant.support.MerchantMessageChatMemoryManager;
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

/**
 * 商家 AI 抽象策略基类。
 */
@Slf4j
public abstract class AbstractMerchantAiStrategy {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    protected static final String DEFAULT_INSTRUCTION = "No additional requirement";
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ChatClient merchantStrategyChatClient;
    protected final IMerchantAiSessionService merchantSessionService;
    protected final IMerchantAiMessageService merchantMessageService;
    protected final MerchantMessageChatMemoryManager memoryManager;
    protected final RemoteShopService remoteShopService;
    protected final IShopRagService shopRagService;
    private final ObjectMapper objectMapper;

    protected AbstractMerchantAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                                         IMerchantAiSessionService merchantSessionService,
                                         IMerchantAiMessageService merchantMessageService,
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

    /**
     * 返回字符串数据流。
     */
    public final Flux<String> execute(Long userId, MerchantChatDTO dto) {
        validateInput(userId, dto);
        MerchantAiSession session = merchantSessionService.getAndCheckSession(userId, dto.getSessionId());
        validateSession(dto, session);
        validateSceneInput(dto, session);

        String conversationId = buildConversationId(dto.getSessionId());
        memoryManager.rebuildConversationMemory(conversationId, dto.getSessionId());

        MerchantAiMessage userRecord = merchantMessageService.saveMessage(
                dto.getSessionId(),
                "user",
                resolveRawUserMessage(dto),
                dto.getReviewId(),
                dto.getProductId(),
                resolveMessageTimeRange(dto),
                resolveMessageAnalysisRecordId(dto)
        );
        memoryManager.appendMessageToCache(userRecord);
        touchSession(dto.getSessionId());

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(MerchantAiPromptConstants.COMMON_SYSTEM_PROMPT),
                new SystemMessage(buildScenePrompt(dto, session)),
                new UserMessage(resolveInstruction(dto))
        ));
        log.info("prompt:{}", prompt);
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

    /**
     * 构建场景提示词。
     */
    protected abstract String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session);

    /**
     * 校验场景入参。
     */
    protected void validateSceneInput(MerchantChatDTO dto, MerchantAiSession session) {
    }

    /**
     * 解析指令。
     */
    protected final String resolveInstruction(MerchantChatDTO dto) {
        String value = firstNonBlank(dto.getInstruction(), dto.getRawMessage(), dto.getMessage());
        return StringUtils.hasText(value) ? value.trim() : DEFAULT_INSTRUCTION;
    }

    /**
     * 获取字符串结果。
     */
    protected final String formatDate(Date date) {
        return date == null ? "No data" : DATE_TIME_FORMAT.format(date);
    }

    /**
     * 获取字符串结果。
     */
    protected final String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "No data";
    }

    /**
     * 获取字符串结果。
     */
    protected final String defaultNumber(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    /**
     * 获取字符串结果。
     */
    protected final String defaultLongNumber(Long value) {
        return value == null ? "0" : String.valueOf(value);
    }

    /**
     * 获取字符串结果。
     */
    protected final String defaultDecimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 获取店铺提示词上下文。
     */
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

    /**
     * 解析原始用户消息。
     */
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

    /**
     * 解析消息时间范围。
     */
    protected final String resolveMessageTimeRange(MerchantChatDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getType())) {
            return null;
        }
        String normalizedType = dto.getType().trim().toLowerCase(Locale.ROOT);
        if (!"analysis".equals(normalizedType)) {
            return null;
        }
        return dto.getTimeRange();
    }

    /**
     * 解析消息分析记录 ID。
     */
    protected final Long resolveMessageAnalysisRecordId(MerchantChatDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getType())) {
            return null;
        }
        String normalizedType = dto.getType().trim().toLowerCase(Locale.ROOT);
        if (!"analysis".equals(normalizedType)) {
            return null;
        }
        return dto.getAnalysisRecordId();
    }

    /**
     * 获取字符串结果。
     */
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

    /**
     * 保存助手消息。
     */
    private void saveAssistantMessage(MerchantChatDTO dto, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        MerchantAiMessage assistantRecord = merchantMessageService.saveMessage(
                dto.getSessionId(),
                "assistant",
                content,
                dto.getReviewId(),
                dto.getProductId(),
                resolveMessageTimeRange(dto),
                resolveMessageAnalysisRecordId(dto)
        );
        memoryManager.appendMessageToCache(assistantRecord);
        touchSession(dto.getSessionId());
    }

    /**
     * 刷新会话时间。
     */
    private void touchSession(Long sessionId) {
        MerchantAiSession session = new MerchantAiSession();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        merchantSessionService.updateById(session);
    }

    /**
     * 校验输入参数。
     */
    private void validateInput(Long userId, MerchantChatDTO dto) {
        if (userId == null) {
            throw new ServiceException("User not logged in");
        }
        if (dto == null || dto.getSessionId() == null || dto.getShopId() == null || !StringUtils.hasText(dto.getType())) {
            throw new ServiceException("Invalid request");
        }
    }

    /**
     * 校验会话。
     */
    private void validateSession(MerchantChatDTO dto, MerchantAiSession session) {
        if (!dto.getShopId().equals(session.getShopId())) {
            throw new ServiceException("Session and shop mismatch");
        }
        if (!StringUtils.hasText(session.getType()) || !session.getType().equalsIgnoreCase(dto.getType().trim())) {
            throw new ServiceException("Session and type mismatch");
        }
    }

    /**
     * 构建会话 ID。
     */
    private String buildConversationId(Long sessionId) {
        return "merchant::session::" + sessionId;
    }

    /**
     * 解析店铺类型名称。
     */
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

    /**
     * 店铺提示词上下文类。
     */
    @Getter
    protected static final class ShopPromptContext {

        private final Long shopId;
        private final String shopName;
        private final String shopType;
        private final ShopDTO shopDTO;
        private final ShopVO ragShop;

        /**
         * 构造店铺提示词上下文。
         */
        private ShopPromptContext(Long shopId, String shopName, String shopType, ShopDTO shopDTO, ShopVO ragShop) {
            this.shopId = shopId;
            this.shopName = shopName;
            this.shopType = shopType;
            this.shopDTO = shopDTO;
            this.ragShop = ragShop;
        }
    }
}
