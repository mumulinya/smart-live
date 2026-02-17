package com.smartLive.wallet.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 统一下单响应VO
 *
 * @author smartLive
 */
@Data
public class UnifiedPayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付流水号 */
    private String paySn;

    /** H5支付跳转URL */
    private String mwebUrl;

    /** Native支付二维码链接 */
    private String codeUrl;

    /** Native支付二维码Base64图片 */
    private String codeImgBase64;

    /** App/小程序支付参数 */
    private Map<String, String> payParams;
}
