package com.smartLive.ai.strategy.milvus.factory;

import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class MilvusSyncFactory {
    @Bean
    public Map<Integer, MilvusSyncStrategy> milvusStrategyMap(List<MilvusSyncStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        MilvusSyncStrategy::getType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
