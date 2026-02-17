package com.smartLive.wallet.strategy;

import com.smartLive.common.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付策略工厂
 *
 * @author smartLive
 */
@Component
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        for (PaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.getPayMethod(), strategy);
        }
    }

    /**
     * 获取支付策略
     *
     * @param payMethod 支付方式 (wechat/alipay)
     * @return 策略实现
     */
    public PaymentStrategy getStrategy(String payMethod) {
        PaymentStrategy strategy = strategyMap.get(payMethod);
        if (strategy == null) {
            throw new BusinessException("不支持的支付方式: " + payMethod);
        }
        return strategy;
    }
}
