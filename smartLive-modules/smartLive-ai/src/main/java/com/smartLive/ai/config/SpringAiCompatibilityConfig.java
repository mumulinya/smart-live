package com.smartLive.ai.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 兼容配置类。
 */
@Configuration
public class SpringAiCompatibilityConfig {

    /**
     * 应用 Spring AI JSON 兼容设置。
     */
    @PostConstruct
    public void applySpringAiJsonCompatibility() {
        // 上游模型提供方可能会返回额外的枚举值（例如中止状态）。
        // 忽略未知枚举值，避免流式解析因兼容性问题中断。
        ModelOptionsUtils.OBJECT_MAPPER.configure(
                DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                true
        );
    }

    /**
     * 构建 Jackson 枚举兼容定制器。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonEnumCompatibilityCustomizer() {
        return builder -> builder.featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }
}
