package com.smartLive.interaction.config;

import com.smartLive.interaction.strategy.follow.FollowBaseStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * @Description: 关注策略工厂
 * @Author: lizhong.li
 * @Date: 2022/9/5 17:01
 */
@Configuration
public class FollowStrategyFactory {
    @Bean
    public Map<String, FollowBaseStrategy> FollowStrategyMap(List<FollowBaseStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        FollowBaseStrategy::getType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
