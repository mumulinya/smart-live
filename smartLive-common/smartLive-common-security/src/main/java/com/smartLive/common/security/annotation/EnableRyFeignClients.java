package com.smartLive.common.security.annotation;

import com.smartLive.common.security.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;
import java.lang.annotation.*;

/**
 * 自定义feign注解
 * 添加basePackages路径
 * 
 * @author smartLive
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableFeignClients(defaultConfiguration = DefaultFeignConfig.class)
public @interface EnableRyFeignClients
{
    String[] value() default {};

    String[] basePackages() default { "com.smartLive"};

    Class<?>[] basePackageClasses() default {};

    Class<?>[] defaultConfiguration() default {};

    Class<?>[] clients() default {};
}
