package com.smartLive.search.strategy.factory;

import com.smartLive.search.strategy.EsSyncStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * @Description: EsSyncStrategy工厂类
 * @Author: lizhong.li
 * @CreateDate: 2020/7/27 16:01
 */
@Configuration
public class EsSyncStrategyFactory {
    @Bean
    public Map<Integer, EsSyncStrategy> esStrategyMap(List<EsSyncStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        EsSyncStrategy::getType,  // 使用 dataType 作为键
                        Function.identity()               // 策略对象作为值
                ));
    }
}
