package com.smartLive.search.config;

import com.smartLive.search.strategy.EsSyncStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class EsSyncStrategyFactory {
    @Bean
    public Map<String, EsSyncStrategy> esStrategyMap(List<EsSyncStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        EsSyncStrategy::getDataType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
