package com.smartLive.wallet.service.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.wallet.config.AlipayProperties;
import com.smartLive.wallet.config.WechatPayProperties;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import com.smartLive.wallet.service.IPayService;
import com.smartLive.wallet.service.IWalletService;
import com.smartLive.wallet.strategy.PaymentStrategy;
import com.smartLive.wallet.strategy.PaymentStrategyFactory;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务实现 (Strategy Pattern)
 *
 * @author smartLive
 */
@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    /** 支付状态常量 */
    private static final int PAY_STATUS_PENDING = 0;
    private static final int PAY_STATUS_SUCCESS = 1;
    private static final int PAY_STATUS_FAILED = 2;
    private static final int PAY_STATUS_CANCELED = 3;

    /** 业务类型常量 */
    private static final String BIZ_TYPE_RECHARGE = "recharge";
    private static final String BIZ_TYPE_ORDER = "order";

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private WechatPayProperties wechatPayProperties;
    
    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired(required = false)
    private NotificationParser notificationParser;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private RemoteOrderService remoteOrderService;

    // ==========================================
    // 1. 统一下单
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnifiedPayVO unifiedOrder(Long userId, UnifiedPayDTO dto) {
        // 1. 参数校验
        validatePayRequest(dto);

        // 2. 校验业务单据，获取金额
        BigDecimal payAmount = resolvePayAmount(userId, dto);

        // 3. 检查是否存在未支付的同业务单
        PaymentRecord existingRecord = findPendingRecord(dto.getBizType(), dto.getBizId());
        if (existingRecord != null) {
            log.info("存在未支付的支付记录, paySn={}, bizType={}, bizId={}",
                    existingRecord.getPaySn(), dto.getBizType(), dto.getBizId());
            // 复用已有的支付记录, 调用策略
            PaymentStrategy strategy = paymentStrategyFactory.getStrategy(existingRecord.getPayMethod());
            return strategy.unifiedOrder(existingRecord, dto);
        }

        // 4. 生成支付流水号并落库
        String paySn = generatePaySn();
        PaymentRecord record = new PaymentRecord();
        record.setPaySn(paySn);
        record.setUserId(userId);
        record.setBizType(dto.getBizType());
        record.setBizId(dto.getBizId());
        record.setAmount(payAmount);
        record.setPayMethod(StringUtils.isEmpty(dto.getPayMethod()) ? "wechat" : dto.getPayMethod());
        record.setStatus(PAY_STATUS_PENDING);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insert(record);

        log.info("创建支付记录成功, paySn={}, userId={}, bizType={}, bizId={}, amount={}, method={}",
                paySn, userId, dto.getBizType(), dto.getBizId(), payAmount, record.getPayMethod());

        // 5. 发送延迟消息，超时自动取消支付
        mqMessageSendUtils.sendMqMessage(
                OrderMqConstants.PAY_DELAY_EXCHANGE,
                OrderMqConstants.PAY_DELAY_ROUTING_KEY,
                record.getId(),
                OrderMqConstants.PAY_DELAY_TIME);
        log.info("已发送支付超时延迟消息, paySn={}, recordId={}, delay={}ms", paySn, record.getId(), OrderMqConstants.PAY_DELAY_TIME);

        // 6. 调用策略下单
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(record.getPayMethod());
        return strategy.unifiedOrder(record, dto);
    }

    // ==========================================
    // 2. 查询支付状态
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayStatusVO queryPayStatus(String paySn) {
        if (StringUtils.isEmpty(paySn)) {
            throw new BusinessException("支付流水号不能为空");
        }
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }

        // 如果已经是终态 (成功/失败)，直接返回数据库状态
        if (record.getStatus() != null && record.getStatus() != 0) {
            PayStatusVO vo = new PayStatusVO();
            vo.setStatus(record.getStatus());
            return vo;
        }

        // 待支付状态 → 主动查询第三方支付平台
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(record.getPayMethod());
        PayStatusVO vo = strategy.queryPayStatus(record);

        // 如果查询结果为已支付，更新数据库并触发业务
        if (vo.getStatus() != null && vo.getStatus() == PAY_STATUS_SUCCESS) {
            log.info("主动查询确认支付成功, paySn={}, transactionId={}", paySn, vo.getTransactionId());
            try {
                processCallback(paySn, vo.getTransactionId(), record.getAmount(), true);
                log.info("充值业务处理完成, paySn={}", paySn);
            } catch (Exception e) {
                log.error("主动查询后处理回调异常, paySn={}", paySn, e);
                // 即使处理失败也返回支付成功状态，下次轮询会再尝试
            }
        }

        return vo;
    }

    @Override
    public void cancelPay(Long userId, String paySn) {
        if (StringUtils.isEmpty(paySn)) {
            throw new BusinessException("支付流水号不能为空");
        }
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        // 校验是否是当前用户的支付记录
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此支付记录");
        }
        // 只有待支付状态才能取消
        if (record.getStatus() == null || record.getStatus() != PAY_STATUS_PENDING) {
            throw new BusinessException("当前状态不可取消");
        }

        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, record.getId())
                .eq(PaymentRecord::getStatus, PAY_STATUS_PENDING)
                .set(PaymentRecord::getStatus, PAY_STATUS_CANCELED)
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now());
        int rows = paymentRecordMapper.update(null, update);
        if (rows > 0) {
            log.info("用户主动取消支付, paySn={}, userId={}", paySn, userId);
        } else {
            throw new BusinessException("取消失败，支付状态已变更");
        }
    }

    // ==========================================
    // 3. 微信回调处理
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWechatCallback(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 读取请求体
            String body = readRequestBody(request);
            log.info("收到微信支付回调");

            // 2. 构造验签参数
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader("Wechatpay-Serial"))
                    .nonce(request.getHeader("Wechatpay-Nonce"))
                    .timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .signature(request.getHeader("Wechatpay-Signature"))
                    .body(body)
                    .build();

            // 3. 验签并解析
            if (notificationParser == null) {
                log.error("微信支付 SDK 未初始化");
                writeCallbackResponse(response, 500, "微信支付未配置");
                return;
            }
            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);

            // 4. 获取支付流水号
            String paySn = transaction.getOutTradeNo();
            processCallback(paySn, transaction.getTransactionId(), 
                    new BigDecimal(transaction.getAmount().getTotal()).divide(new BigDecimal("100")), 
                    transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS);

            writeCallbackResponse(response, 200, "SUCCESS");

        } catch (Exception e) {
            log.error("处理微信支付回调异常: {}", e.getMessage(), e);
            writeCallbackResponse(response, 500, "FAIL");
        }
    }

    // ==========================================
    // 4. 支付宝回调处理
    // ==========================================

    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调");
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        try {
            // 1. 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(params, 
                    alipayProperties.getPublicKey(), 
                    alipayProperties.getCharset(), 
                    alipayProperties.getSignType());

            if (!signVerified) {
                log.error("支付宝回调验签失败");
                return "fail";
            }

            // 2. 获取参数
            String paySn = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            // 3. 处理业务
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                processCallback(paySn, tradeNo, new BigDecimal(totalAmount), true);
            }

            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "fail";
        }
    }

    // ==========================================
    // 私有通用方法
    // ==========================================

    /**
     * 通用回调业务处理
     */
    private void processCallback(String paySn, String transactionId, BigDecimal callbackAmount, boolean isSuccess) {
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            log.error("回调中支付记录不存在, paySn={}", paySn);
            throw new BusinessException("支付记录不存在");
        }

        // 幂等检查
        if (record.getStatus() == PAY_STATUS_SUCCESS || record.getStatus() == PAY_STATUS_CANCELED) {
            log.info("支付已处理, paySn={}, status={}", paySn, record.getStatus());
            return;
        }

        if (isSuccess) {
            // 金额校验
            if (record.getAmount().compareTo(callbackAmount) != 0) {
                log.error("金额不一致, paySn={}, callback={}, record={}", paySn, callbackAmount, record.getAmount());
                throw new BusinessException("金额不一致");
            }

            // 更新支付记录
            updateRecordSuccess(record.getId(), transactionId);

            // 业务分发
            dispatchBusiness(record);
        } else {
            updateRecordFailed(record.getId());
        }
    }

    /**
     * 参数校验
     */
    private void validatePayRequest(UnifiedPayDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (StringUtils.isEmpty(dto.getBizType())) {
            throw new BusinessException("业务类型不能为空");
        }
        if (!BIZ_TYPE_RECHARGE.equals(dto.getBizType()) && !BIZ_TYPE_ORDER.equals(dto.getBizType())) {
            throw new BusinessException("不支持的业务类型: " + dto.getBizType());
        }
        if (StringUtils.isEmpty(dto.getBizId()) && BIZ_TYPE_ORDER.equals(dto.getBizType())) {
            throw new BusinessException("订单号不能为空");
        }
    }

    /**
     * 根据业务类型解析实际支付金额
     */
    private BigDecimal resolvePayAmount(Long userId, UnifiedPayDTO dto) {
        if (BIZ_TYPE_RECHARGE.equals(dto.getBizType())) {
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("充值金额必须大于0");
            }
            BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (StringUtils.isEmpty(dto.getBizId())) {
                dto.setBizId(generateRechargeBizId());
            }
            return amount;
        } else if (BIZ_TYPE_ORDER.equals(dto.getBizType())) {
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("订单金额异常");
            }
            return dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        }
        throw new BusinessException("不支持的业务类型");
    }

    /**
     * 业务分发
     */
    private void dispatchBusiness(PaymentRecord record) {
        String bizType = record.getBizType();
        log.info("业务分发, bizType={}, bizId={}, amount={}", bizType, record.getBizId(), record.getAmount());

        if (BIZ_TYPE_RECHARGE.equals(bizType)) {
            try {
                walletService.recharge(record.getUserId(), record.getAmount());
                log.info("充值成功, userId={}, amount={}", record.getUserId(), record.getAmount());
            } catch (Exception e) {
                log.error("充值回调处理失败, paySn={}: {}", record.getPaySn(), e.getMessage(), e);
                throw new BusinessException("充值回调处理失败");
            }
        } else if (BIZ_TYPE_ORDER.equals(bizType)) {
            try {
                // 1. 记录账单流水
                walletService.recordOrderPayment(
                        record.getUserId(),
                        record.getAmount(),
                        record.getBizId(),
                        record.getPayMethod()
                );

                // 2. 映射支付方式到 PayTypeConstants
                int payType = mapPayType(record.getPayMethod());

                // 3. Feign调用订单模块更新订单状态
                Long orderId = Long.parseLong(record.getBizId());
                Integer result = remoteOrderService.paySuccess(orderId, payType);
                if (result == null || result <= 0) {
                    log.error("Feign调用订单模块失败, orderId={}", orderId);
                    throw new BusinessException("更新订单状态失败");
                }
                log.info("订单支付成功, orderId={}, payType={}", orderId, payType);
            } catch (NumberFormatException e) {
                log.error("订单ID格式错误, bizId={}: {}", record.getBizId(), e.getMessage(), e);
                throw new BusinessException("订单ID格式错误");
            } catch (Exception e) {
                log.error("订单支付回调处理失败, paySn={}: {}", record.getPaySn(), e.getMessage(), e);
                throw new BusinessException("订单支付回调处理失败");
            }
        }
    }

    /**
     * 映射支付方式字符串到PayTypeConstants
     */
    private int mapPayType(String payMethod) {
        if ("alipay".equals(payMethod)) {
            return PayTypeConstants.ALIPAY;
        } else if ("wechat".equals(payMethod)) {
            return PayTypeConstants.WECHAT;
        } else if ("balance".equals(payMethod)) {
            return PayTypeConstants.BALANCE;
        }
        return PayTypeConstants.ALIPAY; // 默认支付宝
    }

    /**
     * 更新支付记录为成功
     */
    private void updateRecordSuccess(Long recordId, String transactionId) {
        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, recordId)
                .set(PaymentRecord::getStatus, PAY_STATUS_SUCCESS)
                .set(PaymentRecord::getTransactionId, transactionId)
                .set(PaymentRecord::getPayTime, LocalDateTime.now())
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now());
        paymentRecordMapper.update(null, update);
    }

    /**
     * 更新支付记录为失败
     */
    private void updateRecordFailed(Long recordId) {
        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, recordId)
                .set(PaymentRecord::getStatus, PAY_STATUS_FAILED)
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now());
        paymentRecordMapper.update(null, update);
    }

    private PaymentRecord findByPaySn(String paySn) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getPaySn, paySn);
        return paymentRecordMapper.selectOne(wrapper);
    }

    private PaymentRecord findPendingRecord(String bizType, String bizId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getBizType, bizType)
                .eq(PaymentRecord::getBizId, bizId)
                .eq(PaymentRecord::getStatus, PAY_STATUS_PENDING)
                .orderByDesc(PaymentRecord::getCreateTime)
                .last("LIMIT 1");
        return paymentRecordMapper.selectOne(wrapper);
    }

    private String generatePaySn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + random;
    }

    private String generateRechargeBizId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "R" + date + random;
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("读取请求体失败: {}", e.getMessage());
        }
        return sb.toString();
    }

    private void writeCallbackResponse(HttpServletResponse response, int statusCode, String message) {
        try {
            response.setStatus(statusCode);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + (statusCode == 200 ? "SUCCESS" : "FAIL") + "\",\"message\":\"" + message + "\"}");
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("写入回调响应失败: {}", e.getMessage());
        }
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<PaymentRecord> getPayList(Long userId, Integer page, Integer pageSize, Integer status) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getUserId, userId);
        if (status != null) {
            wrapper.eq(PaymentRecord::getStatus, status);
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PaymentRecord> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        return paymentRecordMapper.selectPage(pageParam, wrapper);
    }
}
