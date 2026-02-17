package com.smartLive.wallet.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.wallet.config.WechatPayProperties;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import com.smartLive.wallet.strategy.PaymentStrategy;
import com.wechat.pay.java.service.payments.app.AppService;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.h5.model.SceneInfo;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付策略实现
 *
 * @author smartLive
 */
@Slf4j
@Service
public class WechatPaymentStrategy implements PaymentStrategy {

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired(required = false)
    private H5Service h5Service;

    @Autowired(required = false)
    private JsapiService jsapiService;

    @Autowired(required = false)
    private AppService appService;

    @Autowired(required = false)
    private NativePayService nativePayService;

    @Override
    public String getPayMethod() {
        return "wechat";
    }

    @Override
    public UnifiedPayVO unifiedOrder(PaymentRecord record, UnifiedPayDTO dto) {
        UnifiedPayVO vo = new UnifiedPayVO();
        vo.setPaySn(record.getPaySn());

        // 金额转分
        int totalFen = record.getAmount().multiply(new BigDecimal("100")).intValue();
        String description = "recharge".equals(record.getBizType()) ? "钱包充值" : "订单支付";
        String appType = dto.getAppType();

        if (wechatPayProperties.getMchId() == null || wechatPayProperties.getMchId().isEmpty()) {
            // Mock
            log.warn("微信支付未配置，返回模拟支付参数, paySn={}", record.getPaySn());
            Map<String, String> mockParams = new HashMap<>();
            mockParams.put("paySn", record.getPaySn());
            mockParams.put("mockMode", "true");
            mockParams.put("amount", record.getAmount().toPlainString());
            vo.setPayParams(mockParams);
            if ("native".equalsIgnoreCase(appType)) {
                vo.setCodeUrl("weixin://wxpay/bizpayurl?pr=mock_code_url");
                // 模拟二维码 Base64
                try {
                    String base64Img = cn.hutool.extra.qrcode.QrCodeUtil.generateAsBase64("weixin://wxpay/bizpayurl?pr=mock_code_url", new cn.hutool.extra.qrcode.QrConfig(300, 300), "png");
                    vo.setCodeImgBase64(base64Img);
                } catch (Exception e) {
                   // ignore
                }
            }
            return vo;
        }

        try {
            if ("h5".equalsIgnoreCase(appType)) {
                vo = prepayH5(record, totalFen, description);
            } else if ("miniapp".equalsIgnoreCase(appType) || "jsapi".equalsIgnoreCase(appType)) {
                vo = prepayJsapi(record, totalFen, description);
            } else if ("native".equalsIgnoreCase(appType)) {
                vo = prepayNative(record, totalFen, description);
            } else {
                vo = prepayApp(record, totalFen, description);
            }
        } catch (Exception e) {
            log.error("调用微信下单失败, paySn={}: {}", record.getPaySn(), e.getMessage(), e);
            throw new BusinessException("调用微信支付失败，请稍后重试");
        }
        
        vo.setPaySn(record.getPaySn());
        return vo;
    }

    @Override
    public PayStatusVO queryPayStatus(PaymentRecord record) {
        PayStatusVO vo = new PayStatusVO();
        vo.setStatus(record.getStatus());
        return vo;
    }

    // ==========================================
    // 私有下单方法 (从 PayServiceImpl 迁移)
    // ==========================================

    private UnifiedPayVO prepayNative(PaymentRecord record, int totalFen, String description) {
        PrepayRequest request = new PrepayRequest();
        request.setAppid(wechatPayProperties.getAppId());
        request.setMchid(wechatPayProperties.getMchId());
        request.setDescription(description);
        request.setOutTradeNo(record.getPaySn());
        request.setNotifyUrl(wechatPayProperties.getNotifyUrl());

        Amount amount = new Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        PrepayResponse response = nativePayService.prepay(request);

        UnifiedPayVO vo = new UnifiedPayVO();
        vo.setCodeUrl(response.getCodeUrl());
        // 生成二维码 Base64 图片
        try {
            String base64Img = cn.hutool.extra.qrcode.QrCodeUtil.generateAsBase64(response.getCodeUrl(), new cn.hutool.extra.qrcode.QrConfig(300, 300), "png");
            vo.setCodeImgBase64(base64Img);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
        }
        return vo;
    }

    private UnifiedPayVO prepayH5(PaymentRecord record, int totalFen, String description) {
        com.wechat.pay.java.service.payments.h5.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();
        request.setAppid(wechatPayProperties.getAppId());
        request.setMchid(wechatPayProperties.getMchId());
        request.setDescription(description);
        request.setOutTradeNo(record.getPaySn());
        request.setNotifyUrl(wechatPayProperties.getNotifyUrl());

        com.wechat.pay.java.service.payments.h5.model.Amount amount =
                new com.wechat.pay.java.service.payments.h5.model.Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        SceneInfo sceneInfo = new SceneInfo();
        sceneInfo.setPayerClientIp("127.0.0.1");
        H5Info h5Info = new H5Info();
        h5Info.setType("Wap");
        sceneInfo.setH5Info(h5Info);
        request.setSceneInfo(sceneInfo);

        com.wechat.pay.java.service.payments.h5.model.PrepayResponse response = h5Service.prepay(request);

        UnifiedPayVO vo = new UnifiedPayVO();
        vo.setMwebUrl(response.getH5Url());
        return vo;
    }

    private UnifiedPayVO prepayJsapi(PaymentRecord record, int totalFen, String description) {
        com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
        request.setAppid(wechatPayProperties.getAppId());
        request.setMchid(wechatPayProperties.getMchId());
        request.setDescription(description);
        request.setOutTradeNo(record.getPaySn());
        request.setNotifyUrl(wechatPayProperties.getNotifyUrl());

        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        // JSAPI 需要 openid，这里暂用占位
        com.wechat.pay.java.service.payments.jsapi.model.Payer payer =
                new com.wechat.pay.java.service.payments.jsapi.model.Payer();
        payer.setOpenid("PLACEHOLDER_OPENID");
        request.setPayer(payer);

        com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse response = jsapiService.prepay(request);

        UnifiedPayVO vo = new UnifiedPayVO();
        Map<String, String> params = new HashMap<>();
        params.put("prepayId", response.getPrepayId());
        params.put("appId", wechatPayProperties.getAppId());
        vo.setPayParams(params);
        return vo;
    }

    private UnifiedPayVO prepayApp(PaymentRecord record, int totalFen, String description) {
        com.wechat.pay.java.service.payments.app.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.app.model.PrepayRequest();
        request.setAppid(wechatPayProperties.getAppId());
        request.setMchid(wechatPayProperties.getMchId());
        request.setDescription(description);
        request.setOutTradeNo(record.getPaySn());
        request.setNotifyUrl(wechatPayProperties.getNotifyUrl());

        com.wechat.pay.java.service.payments.app.model.Amount amount =
                new com.wechat.pay.java.service.payments.app.model.Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        com.wechat.pay.java.service.payments.app.model.PrepayResponse response = appService.prepay(request);

        UnifiedPayVO vo = new UnifiedPayVO();
        Map<String, String> params = new HashMap<>();
        params.put("prepayId", response.getPrepayId());
        params.put("appId", wechatPayProperties.getAppId());
        params.put("partnerId", wechatPayProperties.getMchId());
        vo.setPayParams(params);
        return vo;
    }
}
