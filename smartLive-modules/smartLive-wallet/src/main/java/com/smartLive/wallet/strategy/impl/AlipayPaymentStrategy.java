package com.smartLive.wallet.strategy.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.wallet.config.AlipayProperties;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import com.smartLive.wallet.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付策略实现
 *
 * @author smartLive
 */
@Slf4j
@Service
public class AlipayPaymentStrategy implements PaymentStrategy {

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired(required = false)
    private AlipayClient alipayClient;

    @Override
    public String getPayMethod() {
        return "alipay";
    }

    @Override
    public UnifiedPayVO unifiedOrder(PaymentRecord record, UnifiedPayDTO dto) {
        UnifiedPayVO vo = new UnifiedPayVO();
        vo.setPaySn(record.getPaySn());
        String appType = dto.getAppType();
        String subject = "recharge".equals(record.getBizType()) ? "钱包充值" : "订单支付";

        if (alipayClient == null) {
            log.warn("支付宝未配置，返回模拟数据");
            Map<String, String> mockParams = new HashMap<>();
            mockParams.put("paySn", record.getPaySn());
            mockParams.put("mockMode", "true");
            vo.setPayParams(mockParams);
            // 模拟二维码 Base64 (生成一个真实的二维码图片)
            try {
                String base64Img = cn.hutool.extra.qrcode.QrCodeUtil.generateAsBase64("https://opendocs.alipay.com", new cn.hutool.extra.qrcode.QrConfig(300, 300), "png");
                vo.setCodeImgBase64(base64Img);
            } catch (Exception e) {
                // ignore
            }
            return vo;
        }

        try {
            if ("native".equalsIgnoreCase(appType)) {
                // 当面付 - 扫码支付
                AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
                request.setNotifyUrl(alipayProperties.getNotifyUrl());
                AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
                model.setOutTradeNo(record.getPaySn());
                model.setTotalAmount(record.getAmount().toPlainString());
                model.setSubject(subject);
                request.setBizModel(model);

                AlipayTradePrecreateResponse response = alipayClient.execute(request);
                if (response.isSuccess()) {
                    vo.setCodeUrl(response.getQrCode());
                    // 生成二维码 Base64 图片
                    try {
                        String base64Img = cn.hutool.extra.qrcode.QrCodeUtil.generateAsBase64(response.getQrCode(), new cn.hutool.extra.qrcode.QrConfig(300, 300), "png");
                        vo.setCodeImgBase64(base64Img);
                    } catch (Exception e) {
                        log.error("生成二维码失败", e);
                    }
                } else {
                    throw new BusinessException("支付宝下单失败: " + response.getSubMsg());
                }

            } else if ("h5".equalsIgnoreCase(appType)) {
                // 手机网站支付
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                request.setNotifyUrl(alipayProperties.getNotifyUrl());
                // 同步跳转地址(支付成功后跳回前端页面)
                // request.setReturnUrl("https://your-frontend.com/pay/result"); 

                AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
                model.setOutTradeNo(record.getPaySn());
                model.setTotalAmount(record.getAmount().toPlainString());
                model.setSubject(subject);
                model.setProductCode("QUICK_WAP_WAY");
                request.setBizModel(model);

                // SDK生成 HTML 表单
                String form = alipayClient.pageExecute(request).getBody();
                
                // 将 form 放入 map 返回给前端
                Map<String, String> data = new HashMap<>();
                data.put("form", form);
                vo.setPayParams(data);

            } else {
                // 默认电脑网站支付 (Page Pay)
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                request.setNotifyUrl(alipayProperties.getNotifyUrl());
                
                AlipayTradePagePayModel model = new AlipayTradePagePayModel();
                model.setOutTradeNo(record.getPaySn());
                model.setTotalAmount(record.getAmount().toPlainString());
                model.setSubject(subject);
                model.setProductCode("FAST_INSTANT_TRADE_PAY");
                request.setBizModel(model);

                String form = alipayClient.pageExecute(request).getBody();
                
                Map<String, String> data = new HashMap<>();
                data.put("form", form);
                vo.setPayParams(data);
            }

        } catch (Exception e) {
            log.error("支付宝下单异常", e);
            throw new BusinessException("支付宝下单失败");
        }

        return vo;
    }

    @Override
    public PayStatusVO queryPayStatus(PaymentRecord record) {
        PayStatusVO vo = new PayStatusVO();
        // 如果已经是终态，直接返回
        if (record.getStatus() != null && record.getStatus() != 0) {
            vo.setStatus(record.getStatus());
            return vo;
        }

        // 主动查询支付宝订单状态
        if (alipayClient != null) {
            try {
                com.alipay.api.request.AlipayTradeQueryRequest request = new com.alipay.api.request.AlipayTradeQueryRequest();
                com.alipay.api.domain.AlipayTradeQueryModel model = new com.alipay.api.domain.AlipayTradeQueryModel();
                model.setOutTradeNo(record.getPaySn());
                request.setBizModel(model);

                com.alipay.api.response.AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response.isSuccess()) {
                    String tradeStatus = response.getTradeStatus();
                    log.info("支付宝主动查询, paySn={}, tradeStatus={}", record.getPaySn(), tradeStatus);
                    if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                        vo.setStatus(1); // 支付成功
                        vo.setTransactionId(response.getTradeNo());
                    } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                        vo.setStatus(3); // 支付取消/关闭
                    } else {
                        vo.setStatus(0); // 待支付 (WAIT_BUYER_PAY)
                    }
                } else {
                    log.warn("支付宝查询失败, paySn={}, subMsg={}", record.getPaySn(), response.getSubMsg());
                    vo.setStatus(record.getStatus());
                }
            } catch (Exception e) {
                log.error("支付宝主动查询异常, paySn={}", record.getPaySn(), e);
                vo.setStatus(record.getStatus());
            }
        } else {
            vo.setStatus(record.getStatus());
        }
        return vo;
    }
}
