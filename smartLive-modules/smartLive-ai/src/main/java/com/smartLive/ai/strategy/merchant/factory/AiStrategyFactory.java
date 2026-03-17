package com.smartLive.ai.strategy.merchant.factory;

import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AI 策略工厂类。
 */
@Component
public class AiStrategyFactory {

    private static final Map<String, String> TYPE_MAP = Map.of(
            "reply", "replyStrategy",
            "analysis", "analysisStrategy",
            "copywrite", "copywriteStrategy",
            "suggest", "suggestStrategy"
    );

    private final Map<String, AbstractMerchantAiStrategy> strategyMap;

    /**
     * 构造 AI 策略工厂。
     */
    public AiStrategyFactory(Map<String, AbstractMerchantAiStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    /**
     * 获取策略。
     */
    public AbstractMerchantAiStrategy getStrategy(String type) {
        if (!StringUtils.hasText(type)) {
            throw new ServiceException("AI type cannot be empty");
        }
        String normalizedType = type.trim().toLowerCase();
        String beanName = TYPE_MAP.get(normalizedType);
        if (beanName == null) {
            throw new ServiceException("Unsupported AI type: " + type);
        }
        AbstractMerchantAiStrategy strategy = strategyMap.get(beanName);
        if (strategy == null) {
            throw new ServiceException("AI strategy not found: " + beanName);
        }
        return strategy;
    }
}
