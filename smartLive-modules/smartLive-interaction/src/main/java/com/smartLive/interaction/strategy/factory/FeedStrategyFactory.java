package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.comment.CommentStrategy;
import com.smartLive.interaction.strategy.feed.FeedStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: 动态策略工厂
 * @Author: lizhong.li
 * @Date: 2022/9/5 17:01
 */
@Configuration
@Slf4j
public class FeedStrategyFactory {
    @Bean
    public Map<Integer, FeedStrategy> feedStrategyMap(List<FeedStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        FeedStrategy::getType,  // 使用 type 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
