package com.smartLive.wallet.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.app.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付V3 SDK配置
 *
 * @author smartLive
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "wechat.pay", name = "mch-id")
public class WechatPayConfig {

    @Bean
    public Config wechatPayConfig(WechatPayProperties properties) {
        log.info("初始化微信支付配置, mchId={}", properties.getMchId());
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.getMchId())
                .privateKeyFromPath(properties.getPrivateKeyPath())
                .merchantSerialNumber(properties.getSerialNumber())
                .apiV3Key(properties.getApiV3Key())
                .build();
    }

    @Bean
    public H5Service h5Service(Config config) {
        return new H5Service.Builder().config(config).build();
    }

    @Bean
    public JsapiService jsapiService(Config config) {
        return new JsapiService.Builder().config(config).build();
    }

    @Bean
    public AppService appService(Config config) {
        return new AppService.Builder().config(config).build();
    }

    @Bean
    public com.wechat.pay.java.service.payments.nativepay.NativePayService nativePayService(Config config) {
        return new com.wechat.pay.java.service.payments.nativepay.NativePayService.Builder().config(config).build();
    }

    @Bean
    public NotificationParser notificationParser(Config config) {
        return new NotificationParser((NotificationConfig) config);
    }
}
