package com.smartLive.ai.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIClient {

    private static final String FALLBACK_MESSAGE = "Sorry, current model response failed. Please try again later.";
    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient restaurantChatClient;

    public Flux<String> generate(String prompt, String chatId) {
        log.info("Calling model with stream mode, chatId={}", chatId);
        return callAIModel(prompt, chatId);
    }

    public String firstGenerate(String prompt) {
        log.info("Calling model with non-stream mode");
        String response = firstCallAIModel(prompt);
        log.info("Model non-stream call completed");
        return response;
    }

    private Flux<String> callAIModel(String prompt, String chatId) {
        AtomicBoolean firstAttemptHasContent = new AtomicBoolean(false);

        return streamCallWithContentGuard(prompt, chatId, firstAttemptHasContent)
                .onErrorResume(ex -> {
                    if (!firstAttemptHasContent.get()) {
                        log.warn("Stream failed or empty before any valid token. Retry stream once. chatId={}", chatId, ex);
                        AtomicBoolean secondAttemptHasContent = new AtomicBoolean(false);
                        return streamCallWithContentGuard(prompt, chatId, secondAttemptHasContent)
                                .onErrorResume(retryEx -> retryWithNonStream(prompt, chatId, retryEx));
                    }
                    return retryWithNonStream(prompt, chatId, ex);
                });
    }

    private Flux<String> streamCallWithContentGuard(String prompt, String chatId, AtomicBoolean hasContent) {
        hasContent.set(false);
        return streamCall(prompt, chatId)
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isBlank()) {
                        hasContent.set(true);
                    }
                })
                .concatWith(Mono.defer(() -> hasContent.get()
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("Stream completed with empty content"))));
    }

    private Flux<String> streamCall(String prompt, String chatId) {
        return Flux.defer(() -> restaurantChatClient.prompt()
                .user(prompt)
                .advisors((ChatClient.AdvisorSpec a) -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content());
    }

    private Flux<String> retryWithNonStream(String prompt, String chatId, Throwable ex) {
        log.error("Stream chat failed. Retry with non-stream call. chatId={}", chatId, ex);
        return Mono.fromCallable(() -> firstCallAIModel(prompt, chatId))
                .flatMapMany(content -> {
                    if (content == null || content.isBlank()) {
                        log.warn("Non-stream retry returned empty content. Use fallback message. chatId={}", chatId);
                        return Flux.just(FALLBACK_MESSAGE);
                    }
                    return Flux.just(content);
                })
                .switchIfEmpty(Flux.just(FALLBACK_MESSAGE))
                .onErrorResume(nonStreamEx -> {
                    log.error("Non-stream retry failed. chatId={}", chatId, nonStreamEx);
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    private String firstCallAIModel(String prompt) {
        return restaurantChatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String firstCallAIModel(String prompt, String chatId) {
        return restaurantChatClient.prompt()
                .user(prompt)
                .advisors((ChatClient.AdvisorSpec a) -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }
}
