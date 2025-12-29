package com.smartLive.ai.config.strategy;

import com.smartLive.ai.service.strategy.milvus.MilvusSyncStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class MilvusSyncFactory {
    @Bean
    public Map<String, MilvusSyncStrategy> milvusStrategyMap(List<MilvusSyncStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        MilvusSyncStrategy::getDataType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
