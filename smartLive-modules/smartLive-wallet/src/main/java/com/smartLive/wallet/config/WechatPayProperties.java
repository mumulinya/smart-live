package com.smartLive.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置属性
 *
 * @author smartLive
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    /** 微信AppID */
    private String appId;

    /** 微信商户号 */
    private String mchId;

    /** APIv3密钥 */
    private String apiV3Key;

    /** 商户私钥文件路径 */
    private String privateKeyPath;

    /** 商户API证书序列号 */
    private String serialNumber;

    /** 支付回调通知URL */
    private String notifyUrl;
}
