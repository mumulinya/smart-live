package com.smartLive.interaction.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;

/**
 * 策略执行器工具类
 * 用于安全地执行策略，避免空指针异常
 */
@Slf4j
public class StrategyExecutor {

    /**
     * 安全执行策略
     * @param strategyMap 策略映射
     * @param type 策略类型
     * @param executor 策略执行函数
     * @param defaultValue 默认返回值
     * @return 执行结果或默认值
     */
    public static <T, R> R executeStrategy(Map<Integer, T> strategyMap, Integer type, Function<T, R> executor, R defaultValue) {
        T strategy = strategyMap.get(type);
        if (strategy == null) {
            log.warn("找不到类型为 {} 的策略", type);
            return defaultValue;
        }
        return executor.apply(strategy);
    }

    /**
     * 安全执行策略，无返回值
     * @param strategyMap 策略映射
     * @param type 策略类型
     * @param executor 策略执行函数
     */
    public static <T> void executeStrategyVoid(Map<Integer, T> strategyMap, Integer type, StrategyConsumer<T> executor) {
        T strategy = strategyMap.get(type);
        if (strategy == null) {
            log.warn("找不到类型为 {} 的策略", type);
            return;
        }
        try {
            executor.accept(strategy);
        } catch (Exception e) {
            log.error("执行策略时发生错误，类型: {}", type, e);
        }
    }

    @FunctionalInterface
    public interface StrategyConsumer<T> {
        void accept(T strategy) throws Exception;
    }
}