package com.smartLive.ai.config.model;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ChatModel resolution and registration.
 * <p>
 * Priority:
 * 1) smartlive.ai.chat-model-bean
 * 2) API key based candidates (OpenAI/Zhipu)
 * 3) auto-discovered ChatModel beans
 */
@Configuration
public class ChatModelConfiguration {

    /**
     * Primary model used by framework agents.
     */
    @Primary
    @Bean("frameworkChatModel")
    public ChatModel frameworkChatModel(ApplicationContext applicationContext, Environment environment) {
        return resolveChatModel(applicationContext, environment);
    }

    private ChatModel resolveChatModel(ApplicationContext applicationContext, Environment environment) {
        String configuredBeanRaw = trimToNull(environment.getProperty("smartlive.ai.chat-model-bean"));
        String configuredBean = normalizeChatModelBeanName(configuredBeanRaw);
        boolean hasOpenAiKey = hasOpenAiKey(environment);
        boolean hasZhipuAiKey = hasZhipuAiKey(environment);

        Set<String> candidates = new LinkedHashSet<>();
        addConfiguredCandidates(candidates, configuredBeanRaw, configuredBean);

        if (hasZhipuAiKey || isZhipuCandidate(configuredBean)) {
            candidates.add("zhiPuAiChatModel");
            candidates.add("zhipuAiChatModel");
        }
        if (hasOpenAiKey || isOpenAiCandidate(configuredBean)) {
            candidates.add("openAiChatModel");
        }

        addDiscoveredCandidates(
                applicationContext,
                candidates,
                hasOpenAiKey,
                hasZhipuAiKey,
                configuredBean
        );

        if (candidates.isEmpty() || isZhipuCandidate(configuredBean)) {
            candidates.add("zhiPuAiChatModel");
            candidates.add("zhipuAiChatModel");
        }

        List<String> errors = new ArrayList<>();
        for (String beanName : candidates) {
            ChatModel model = tryGetChatModel(applicationContext, beanName, errors);
            if (model != null) {
                return model;
            }
        }

        String[] availableBeanNames = applicationContext.getBeanNamesForType(ChatModel.class, false, false);
        String available = Arrays.toString(availableBeanNames);
        throw new IllegalStateException(
                "Cannot resolve ChatModel bean. configured=" + configuredBeanRaw +
                        ", normalized=" + configuredBean +
                        ", checked=" + candidates +
                        ", available=" + available +
                        ", errors=" + errors +
                        ". Ensure spring.ai.zhipuai.api-key is set and spring.ai.zhipuai.chat.enabled is not false."
        );
    }

    private ChatModel tryGetChatModel(ApplicationContext applicationContext, String beanName, List<String> errors) {
        if (!hasText(beanName) || !applicationContext.containsBean(beanName)) {
            return null;
        }
        try {
            return applicationContext.getBean(beanName, ChatModel.class);
        } catch (Exception ex) {
            errors.add(beanName + ": " + rootCauseMessage(ex));
            return null;
        }
    }

    private void addConfiguredCandidates(Set<String> candidates, String configuredBeanRaw, String configuredBean) {
        if (hasText(configuredBeanRaw)) {
            candidates.add(configuredBeanRaw);
        }
        if (hasText(configuredBean)) {
            candidates.add(configuredBean);
        }
        if (isZhipuCandidate(configuredBeanRaw) || isZhipuCandidate(configuredBean)) {
            candidates.add("zhiPuAiChatModel");
            candidates.add("zhipuAiChatModel");
        }
        if (isOpenAiCandidate(configuredBeanRaw) || isOpenAiCandidate(configuredBean)) {
            candidates.add("openAiChatModel");
        }
    }

    private void addDiscoveredCandidates(
            ApplicationContext applicationContext,
            Set<String> candidates,
            boolean hasOpenAiKey,
            boolean hasZhipuAiKey,
            String configuredBean
    ) {
        String[] discoveredBeans = applicationContext.getBeanNamesForType(ChatModel.class, false, false);
        for (String beanName : discoveredBeans) {
            if (isOpenAiCandidate(beanName) && !hasOpenAiKey && !isOpenAiCandidate(configuredBean)) {
                continue;
            }
            if (isZhipuCandidate(beanName) && !hasZhipuAiKey && !isZhipuCandidate(configuredBean)) {
                continue;
            }
            candidates.add(beanName);
        }
    }

    private String normalizeChatModelBeanName(String beanName) {
        if (!hasText(beanName)) {
            return null;
        }
        if ("zhipuAiChatModel".equals(beanName)) {
            return "zhiPuAiChatModel";
        }
        return beanName;
    }

    private boolean isOpenAiCandidate(String beanName) {
        return "openAiChatModel".equals(beanName);
    }

    private boolean isZhipuCandidate(String beanName) {
        return "zhiPuAiChatModel".equals(beanName) || "zhipuAiChatModel".equals(beanName);
    }

    private boolean hasOpenAiKey(Environment environment) {
        return hasText(environment.getProperty("spring.ai.openai.api-key"))
                || hasText(environment.getProperty("spring.ai.openai.chat.api-key"));
    }

    private boolean hasZhipuAiKey(Environment environment) {
        return hasText(environment.getProperty("spring.ai.zhipuai.api-key"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}