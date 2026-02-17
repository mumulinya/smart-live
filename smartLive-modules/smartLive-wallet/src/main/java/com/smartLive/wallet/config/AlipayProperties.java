package com.smartLive.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 *
 * @author smartLive
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** AppID */
    private String appId;

    /** 商户私钥 */
    private String privateKey;

    /** 支付宝公钥 */
    private String publicKey;

    /** 支付宝网关 URL (沙箱环境使用新版地址) */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 签名加签类型 */
    private String signType = "RSA2";

    /** 格式 */
    private String format = "json";

    /** 编码 */
    private String charset = "UTF-8";

    /** 异步通知地址 */
    private String notifyUrl;
}
