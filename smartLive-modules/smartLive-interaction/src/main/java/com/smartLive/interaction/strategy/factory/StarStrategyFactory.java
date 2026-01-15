package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: 收藏策略工厂
 * @Author: lizhong.li
 * @Date: 2022/9/5 17:01
 */
@Configuration
@Slf4j
public class StarStrategyFactory {
    @Bean
    public Map<Integer, StarStrategy> StarStrategyMap(List<StarStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        StarStrategy::getType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
