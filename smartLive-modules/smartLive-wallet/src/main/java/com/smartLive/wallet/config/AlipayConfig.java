package com.smartLive.wallet.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置类
 *
 * @author smartLive
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "alipay", name = "app-id")
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties properties) {
        log.info("初始化支付宝SDK, appId={}", properties.getAppId());

        DefaultAlipayClient client = new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getPublicKey(),
                properties.getSignType()
        );
        // 设置超时时间: 连接 30s, 读取 60s (针对由于代理导致的慢速连接)
        client.setConnectTimeout(30000);
        client.setReadTimeout(60000);
        return client;
    }
}
