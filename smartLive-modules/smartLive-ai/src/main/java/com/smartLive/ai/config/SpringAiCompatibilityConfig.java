package com.smartLive.ai.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiCompatibilityConfig {

    @PostConstruct
    public void applySpringAiJsonCompatibility() {
        // Upstream providers may return additional enum values (e.g. finish_reason=abort).
        // Ignore unknown enum values to keep stream parsing resilient.
        ModelOptionsUtils.OBJECT_MAPPER.configure(
                DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                true
        );
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonEnumCompatibilityCustomizer() {
        return builder -> builder.featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }
}
