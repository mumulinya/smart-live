package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.mapper.MessageMapper;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.ai.service.orchestration.AIChatOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * AI Message Service Implementation
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private AIChatOrchestrator aiChatOrchestrator;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Message> selectMessageList(Integer current, Long sessionId) {
        List<Message> list = query().eq("session_id", sessionId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10))
                .getRecords();
        Collections.reverse(list);
        return list;
    }

    @Override
    public Message saveMessage(Long sessionId, String role, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setType("text");
        message.setCreateTime(new Date());
        this.save(message);

        Session session = new Session();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        sessionService.updateById(session);

        return message;
    }

    @Override
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        Long sessionId = messageDTO.getSessionId();
        Long userId = messageDTO.getUserId();
        String message = messageDTO.getMessage();

        saveMessage(sessionId, "user", message);

        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        request.setSessionId(String.valueOf(sessionId));
        request.setUserId(String.valueOf(userId));
        request.setX(messageDTO.getX());
        request.setY(messageDTO.getY());
        request.setDistrict(messageDTO.getRegion());
        StringBuilder fullResponse = new StringBuilder();

        return aiChatOrchestrator.processMessage(request)
                .map(chunk -> {
                    String safeChunk = chunk == null ? "" : chunk;
                    fullResponse.append(safeChunk);
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(safeChunk)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    String response = fullResponse.toString();
                    String json = extractRecommendationJson(response);
                    if (json == null) {
                        return Flux.empty();
                    }
                    String normalizedJson = normalizeRecommendationJson(json);
                    log.info("Detected recommendation JSON, sending card_render event");
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("card_render")
                            .data(normalizedJson)
                            .build());
                }))
                .doFinally(signalType -> {
                    String responseText = fullResponse.toString();
                    if (responseText.isEmpty()) {
                        return;
                    }
                    try {
                        saveMessage(sessionId, "assistant", responseText);
                        log.info("Saved assistant response for session {}", sessionId);
                    } catch (Exception e) {
                        log.error("Failed to save assistant response", e);
                    }
                });
    }

    private String extractRecommendationJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        int end = content.lastIndexOf("}");
        if (end < 0) {
            return null;
        }

        int braceCount = 0;
        int start = -1;
        for (int i = end; i >= 0; i--) {
            char c = content.charAt(i);
            if (c == '}') {
                braceCount++;
            } else if (c == '{') {
                braceCount--;
                if (braceCount == 0) {
                    start = i;
                    break;
                }
            }
        }

        if (start < 0) {
            return null;
        }

        String json = content.substring(start, end + 1);
        return isRecommendationJson(json) ? json : null;
    }

    private boolean isRecommendationJson(String content) {
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return false;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            return jsonNode.has("recommendations") && jsonNode.has("replyText");
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeRecommendationJson(String json) {
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                return json;
            }

            String type = resolveRecommendationType(root);

            if (!hasText(root.path("type").asText(null))) {
                root.put("type", type);
            }

            JsonNode recommendationsNode = root.get("recommendations");
            if (recommendationsNode instanceof ArrayNode recommendations) {
                for (JsonNode node : recommendations) {
                    if (!(node instanceof ObjectNode recommendation)) {
                        continue;
                    }
                    if (!hasText(recommendation.path("type").asText(null))) {
                        recommendation.put("type", type);
                    }
                }
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("Failed to normalize recommendation JSON, keep original", e);
            return json;
        }
    }

    private String resolveRecommendationType(ObjectNode root) {
        String rootType = root.path("type").asText(null);
        if (hasText(rootType)) {
            return rootType;
        }

        JsonNode recommendationsNode = root.get("recommendations");
        if (recommendationsNode instanceof ArrayNode recommendations) {
            for (JsonNode node : recommendations) {
                if (!(node instanceof ObjectNode recommendation)) {
                    continue;
                }

                String itemType = recommendation.path("type").asText(null);
                if (hasText(itemType)) {
                    return itemType;
                }

                if (looksLikeVoucherRecommendation(recommendation)) {
                    return "voucher";
                }
                if (looksLikeShopRecommendation(recommendation)) {
                    return "shop";
                }
            }
        }

        return "shop";
    }

    private boolean looksLikeVoucherRecommendation(ObjectNode recommendation) {
        return hasAnyField(recommendation,
                "shopId",
                "voucherType",
                "title",
                "rules",
                "payValue",
                "actualValue",
                "stock");
    }

    private boolean looksLikeShopRecommendation(ObjectNode recommendation) {
        return hasAnyField(recommendation,
                "distanceText",
                "avgPrice",
                "openHours",
                "address",
                "x",
                "y",
                "sold");
    }

    private boolean hasAnyField(ObjectNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                if (hasText(value.asText())) {
                    return true;
                }
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
