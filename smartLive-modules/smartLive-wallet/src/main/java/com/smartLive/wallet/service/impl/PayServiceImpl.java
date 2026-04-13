package com.smartLive.wallet.service.impl;

import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.core.constant.PaymentStatusConstants;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.domain.MqSendMode;
import com.smartLive.common.rabbitmq.domain.OrderPaidStatsMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.api.DTO.OrderDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.wallet.config.AlipayProperties;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.PaymentBusinessResult;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import com.smartLive.wallet.service.IPayService;
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
 * 支付基础服务实现类 (采用策略模式)
 * 负责聚合支付下单、异步回调的通用处理逻辑、支付状态的主动查询以及订单超时的延迟任务触发。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Service
public class PayServiceImpl implements IPayService {
    private static final String BIZ_TYPE_RECHARGE = "recharge";
    private static final String BIZ_TYPE_ORDER = "order";

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired(required = false)
    private NotificationParser notificationParser;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private WalletPaymentTxService walletPaymentTxService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnifiedPayVO unifiedOrder(Long userId, UnifiedPayDTO dto) {
        validatePayRequest(dto);

        BigDecimal payAmount = resolvePayAmount(userId, dto);
        PaymentRecord existingRecord = findPendingRecord(dto.getBizType(), dto.getBizId());
        if (existingRecord != null) {
            log.info("存在未支付的支付记录, paySn={}, bizType={}, bizId={}",
                    existingRecord.getPaySn(), dto.getBizType(), dto.getBizId());
            PaymentStrategy strategy = paymentStrategyFactory.getStrategy(existingRecord.getPayMethod());
            return strategy.unifiedOrder(existingRecord, dto);
        }

        String paySn = generatePaySn();
        PaymentRecord record = new PaymentRecord();
        record.setPaySn(paySn);
        record.setUserId(userId);
        record.setBizType(dto.getBizType());
        record.setBizId(dto.getBizId());
        record.setAmount(payAmount);
        record.setPayMethod(StringUtils.isEmpty(dto.getPayMethod()) ? "wechat" : dto.getPayMethod());
        record.setStatus(PaymentStatusConstants.PENDING);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insert(record);

        log.info("创建支付记录成功, paySn={}, userId={}, bizType={}, bizId={}, amount={}, method={}",
                paySn, userId, dto.getBizType(), dto.getBizId(), payAmount, record.getPayMethod());

        mqMessageSendUtils.sendMqMessage(
                OrderMqConstants.PAY_DELAY_EXCHANGE,
                OrderMqConstants.PAY_DELAY_ROUTING_KEY,
                record.getId(),
                OrderMqConstants.PAY_DELAY_TIME,
                MqSendMode.SYNC_RETRY_THROW);
        log.info("已发送支付超时延迟消息, paySn={}, recordId={}, delay={}ms", paySn, record.getId(), OrderMqConstants.PAY_DELAY_TIME);

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(record.getPayMethod());
        return strategy.unifiedOrder(record, dto);
    }

    /**
     * 主动向第三方通道同步支付结果。
     * 解决前端轮询时数据库尚未收到异步回调的问题。
     *
     * @param paySn 支付流水号
     * @return 支付状态响应
     */
    @Override
    public PayStatusVO queryPayStatus(String paySn) {
        if (StringUtils.isEmpty(paySn)) {
            throw new BusinessException("支付流水号不能为空");
        }
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }

        if (record.getStatus() != null && record.getStatus() != PaymentStatusConstants.PENDING) {
            PayStatusVO vo = new PayStatusVO();
            vo.setStatus(record.getStatus());
            return vo;
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(record.getPayMethod());
        PayStatusVO vo = strategy.queryPayStatus(record);

        if (vo.getStatus() != null && vo.getStatus() == PaymentStatusConstants.SUCCESS) {
            log.info("主动查询确认支付成功, paySn={}, transactionId={}", paySn, vo.getTransactionId());
            try {
                PaymentBusinessResult result = walletPaymentTxService.confirmPaymentSuccess(paySn, vo.getTransactionId(), record.getAmount());
                publishOrderPaidStatsIfNecessary(result);
                log.info("主动查询支付确认完成, paySn={}, processed={}", paySn, result.isProcessed());
            } catch (Exception e) {
                log.error("主动查询后处理回调异常, paySn={}", paySn, e);
            }
        }

        return vo;
    }

    /**
     * 用户主动取消待支付流水。
     * 只有待支付状态才允许取消，避免覆盖终态记录。
     *
     * @param userId 当前用户ID
     * @param paySn 支付流水号
     */
    @Override
    public void cancelPay(Long userId, String paySn) {
        if (StringUtils.isEmpty(paySn)) {
            throw new BusinessException("支付流水号不能为空");
        }
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此支付记录");
        }
        if (record.getStatus() == null || record.getStatus() != PaymentStatusConstants.PENDING) {
            throw new BusinessException("当前状态不可取消");
        }

        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, record.getId())
                .eq(PaymentRecord::getStatus, PaymentStatusConstants.PENDING)
                .set(PaymentRecord::getStatus, PaymentStatusConstants.CANCELED)
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now());
        int rows = paymentRecordMapper.update(null, update);
        if (rows > 0) {
            log.info("用户主动取消支付, paySn={}, userId={}", paySn, userId);
        } else {
            throw new BusinessException("取消失败，支付状态已变更");
        }
    }

    /**
     * 微信支付回调处理。
     * 负责验签、解析回调并在支付成功时委托 Seata 主事务完成后续业务。
     *
     * @param request 微信回调请求
     * @param response 微信回调响应
     */
    @Override
    public void handleWechatCallback(HttpServletRequest request, HttpServletResponse response) {
        try {
            String body = readRequestBody(request);
            log.info("收到微信支付回调");

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader("Wechatpay-Serial"))
                    .nonce(request.getHeader("Wechatpay-Nonce"))
                    .timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .signature(request.getHeader("Wechatpay-Signature"))
                    .body(body)
                    .build();

            if (notificationParser == null) {
                log.error("微信支付 SDK 未初始化");
                writeCallbackResponse(response, 500, "微信支付未配置");
                return;
            }
            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);

            String paySn = transaction.getOutTradeNo();
            BigDecimal callbackAmount = new BigDecimal(transaction.getAmount().getTotal()).divide(new BigDecimal("100"));
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                PaymentBusinessResult result = walletPaymentTxService.confirmPaymentSuccess(paySn, transaction.getTransactionId(), callbackAmount);
                publishOrderPaidStatsIfNecessary(result);
            } else {
                walletPaymentTxService.markPaymentFailed(paySn);
            }

            writeCallbackResponse(response, 200, "SUCCESS");
        } catch (Exception e) {
            log.error("处理微信支付回调异常: {}", e.getMessage(), e);
            writeCallbackResponse(response, 500, "FAIL");
        }
    }

    /**
     * 支付宝支付回调处理。
     * 负责验签、识别交易状态并驱动后续支付确认流程。
     *
     * @param request 支付宝回调请求
     * @return success/fail
     */
    @Override
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
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                    alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType());

            if (!signVerified) {
                log.error("支付宝回调验签失败");
                return "fail";
            }

            String paySn = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                PaymentBusinessResult result = walletPaymentTxService.confirmPaymentSuccess(paySn, tradeNo, new BigDecimal(totalAmount));
                publishOrderPaidStatsIfNecessary(result);
            } else {
                walletPaymentTxService.markPaymentFailed(paySn);
            }

            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "fail";
        }
    }

    /**
     * 参数校验。
     *
     * @param dto 支付请求参数
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
     * 根据业务类型解析实际支付金额。
     *
     * @param userId 当前用户ID
     * @param dto 支付请求参数
     * @return 标准化后的支付金额
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
     * 根据支付流水号查询支付记录。
     *
     * @param paySn 支付流水号
     * @return 支付记录
     */
    private PaymentRecord findByPaySn(String paySn) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getPaySn, paySn);
        return paymentRecordMapper.selectOne(wrapper);
    }

    /**
     * 查询同一业务单据下仍处于待支付状态的支付记录。
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 待支付记录
     */
    private PaymentRecord findPendingRecord(String bizType, String bizId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getBizType, bizType)
                .eq(PaymentRecord::getBizId, bizId)
                .eq(PaymentRecord::getStatus, PaymentStatusConstants.PENDING)
                .orderByDesc(PaymentRecord::getCreateTime)
                .last("LIMIT 1");
        return paymentRecordMapper.selectOne(wrapper);
    }

    /**
     * 生成统一支付流水号。
     *
     * @return 支付流水号
     */
    private String generatePaySn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + random;
    }

    /**
     * 生成充值业务单号。
     *
     * @return 充值业务单号
     */
    private String generateRechargeBizId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "R" + date + random;
    }

    /**
     * 在支付主事务成功后发送销量统计消息。
     * 这一步属于缓存/统计链路，失败不回滚主交易。
     *
     * @param result 支付业务执行结果
     */
    private void publishOrderPaidStatsIfNecessary(PaymentBusinessResult result) {
        if (result == null || !result.shouldPublishOrderPaidStats()) {
            return;
        }

        try {
            OrderDTO orderDTO = remoteOrderService.getOrderById(result.getOrderId());
            OrderPaidStatsMessage message = new OrderPaidStatsMessage();
            message.setOrderId(result.getOrderId());
            message.setUserId(result.getUserId());
            message.setPayType(result.getPayType());
            message.setMessageKey(buildMessageKey(result.getOrderId(), result.getPayType()));
            if (orderDTO != null) {
                message.setSourceId(orderDTO.getSourceId());
                message.setVerifyShopId(orderDTO.getVerifyShopId());
                message.setAmount(orderDTO.getAmount());
                if (message.getUserId() == null) {
                    message.setUserId(orderDTO.getUserId());
                }
            } else {
                log.warn("支付成功后未取到订单详情，按最小消息发送统计事件, orderId={}", result.getOrderId());
            }

            mqMessageSendUtils.sendMqMessage(
                    OrderMqConstants.ORDER_PAID_STATS_EXCHANGE,
                    OrderMqConstants.ORDER_PAID_STATS_ROUTING_KEY,
                    message
            );
            log.info("已发送订单支付统计消息, orderId={}, payType={}", result.getOrderId(), result.getPayType());
        } catch (Exception e) {
            log.error("订单支付统计消息发送失败，主交易已提交, orderId={}", result.getOrderId(), e);
        }
    }

    /**
     * 构造订单支付统计消息的业务幂等 Key。
     *
     * @param orderId 订单ID
     * @param payType 支付方式
     * @return 幂等 Key
     */
    private String buildMessageKey(Long orderId, Integer payType) {
        return "order-paid-stats:" + orderId + ":" + payType;
    }

    /**
     * 读取 HTTP 请求体内容。
     *
     * @param request HTTP 请求
     * @return 请求体字符串
     */
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

    /**
     * 返回支付回调响应。
     *
     * @param response 响应对象
     * @param statusCode HTTP 状态码
     * @param message 返回消息
     */
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

    /**
     * 分页查询当前用户的支付流水列表。
     *
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 支付状态
     * @return 分页结果
     */
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
