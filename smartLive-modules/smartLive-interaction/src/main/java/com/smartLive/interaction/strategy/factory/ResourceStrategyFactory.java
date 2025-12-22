package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: 资源策略工厂
 * @Author: lizhong.li
 * @Date: 2022/9/5 17:01
 */
@Configuration
public class ResourceStrategyFactory {
    @Bean
    public Map<String, ResourceStrategy> ResourceStrategyMap(List<ResourceStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        ResourceStrategy::getType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
