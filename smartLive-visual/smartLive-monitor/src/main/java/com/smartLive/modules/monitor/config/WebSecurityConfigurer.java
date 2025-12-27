package com.smartLive.modules.monitor.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * 监控权限配置
 * 
 * @author smartLive
 */
@EnableWebSecurity
public class WebSecurityConfigurer
{
    private final String adminContextPath;

    public WebSecurityConfigurer(AdminServerProperties adminServerProperties)
    {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/");

        http
                // 1. Headers 配置改为 Lambda 写法
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // 2. 授权配置改为 authorizeHttpRequests + requestMatchers
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                adminContextPath + "/assets/**",
                                adminContextPath + "/login",
                                adminContextPath + "/actuator/**",
                                adminContextPath + "/instances/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 3. 登录配置
                .formLogin(form -> form
                        .loginPage(adminContextPath + "/login")
                        .successHandler(successHandler)
                )

                // 4. 登出配置
                .logout(logout -> logout.logoutUrl(adminContextPath + "/logout"))

                // 5. CSRF 配置 (Admin Server 通常需要忽略部分 CSRF)
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        adminContextPath + "/instances",
                        adminContextPath + "/actuator/**"
                ));

        return http.build();
    }
}
