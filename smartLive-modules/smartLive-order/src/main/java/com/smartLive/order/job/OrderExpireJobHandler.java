package com.smartLive.order.job;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.smartLive.chat.api.dto.SystemNoticeCreateDTO;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import com.smartLive.common.core.constant.mq.ChatMqConstants;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ItemActionType;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;
import com.smartLive.product.api.RemoteProductService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 订单模块 XXL-JOB 定时任务处理器
 * 处理订单即将过期提醒和订单过期后的自动退款/作废逻辑
 *
 * @author smartLive
 */
@Component
@Slf4j
public class OrderExpireJobHandler {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private RemoteProductService remoteProductService;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    /** 订单临期提醒窗口：过期前多少天触发通知 (默认3天) */
    private static final int EXPIRE_NOTIFY_DAYS = 3;

    // ==================== 任务一：订单临期提醒 ====================

    /**
     * 订单临期提醒定时任务
     * 建议触发频率：每天一次（例如：每天上午 10:00）
     * 逻辑：找出已支付、且在未来 3 天内过期的订单，发送提醒。
     */
    @XxlJob("orderSoonExpireJobHandler")
    public ReturnT<String> orderSoonExpireJobHandler(String param) throws Exception {
        log.info("======== 触发 XXL-JOB: 订单临期提醒任务 orderSoonExpireJobHandler ========");

        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        // 计算临期窗口的截止时间（当前时间 + 3天）
        Date soonExpireDeadline = new Date(nowTime + EXPIRE_NOTIFY_DAYS * 24L * 60 * 60 * 1000);

        // 查询已支付且即将过期（expireTime 在 [今天, 今天+3天] 之间）的订单
        List<Order> orders = orderService.lambdaQuery()
                .eq(Order::getStatus, OrderStatusConstants.PAID)
                .isNotNull(Order::getExpireTime)
                .gt(Order::getExpireTime, now)                     // 还未过期
                .le(Order::getExpireTime, soonExpireDeadline)      // 即将过期
                .list();

        if (CollUtil.isEmpty(orders)) {
            log.info("没有查到即将在 {} 天内过期的已支付订单", EXPIRE_NOTIFY_DAYS);
            return ReturnT.SUCCESS;
        }

        log.info("查到 {} 个即将过期的订单，准备发出提醒", orders.size());
        int notifiedCount = 0;

        for (Order order : orders) {
            try {
                // 调用 MQ 发送系统通知给 IM 模块
                log.info("【临期提醒】给用户 {} 发送订单 {} 的即将过期提醒", order.getUserId(), order.getId());
                
                String content = String.format("温馨提示：您购买的商品（订单号：%s）还有不足 %d 天即将过期，请尽快前往使用，以免影响您的权益哦~", 
                                             order.getId(), EXPIRE_NOTIFY_DAYS);
                Map<String, Object> map = BeanUtil.beanToMap(order);
                SystemNoticeCreateDTO createDTO = new SystemNoticeCreateDTO();
                createDTO.setUserId(order.getUserId());
                createDTO.setSourceType(ResourceTypeConstants.ORDER_CODE);
                createDTO.setContent(content);
                createDTO.setTitle("订单过期提醒");
                createDTO.setAction("order_expire");
                createDTO.setExtraData(map);
                // 复用系统的内部通知结构发送MQ
                mqMessageSendUtils.sendMqMessage(
                        ChatMqConstants.SYSTEM_NOTICE_EXCHANGE,
                        ChatMqConstants.SYSTEM_NOTICE_ROUTING,
                        createDTO
                );
                
                notifiedCount++;
            } catch (Exception e) {
                log.error("给订单 {} 发送临期提醒失败", order.getId(), e);
            }
        }

        log.info("======== 订单临期提醒任务执行结束，共计通知 {} 单 ========", notifiedCount);
        return ReturnT.SUCCESS;
    }

    // ==================== 任务二：订单过期作废与退款 ====================

    /**
     * 订单自动过期处理任务
     * 建议触发频率：每天一次（例如：每天凌晨 01:00）
     * 逻辑：找出已支付且过期时间（expireTime）已经过去的订单，修改状态为“已过期”，并触发退款
     */
    @XxlJob("orderExpireJobHandler")
    public ReturnT<String> orderExpireJobHandler(String param) throws Exception {
        log.info("======== 触发 XXL-JOB: 订单过期自动处理任务 orderExpireJobHandler ========");

        Date now = new Date();

        // 1. 查询已过期（expireTime < 当前时间）且状态仍为已支付(2)的未使用订单
        List<Order> orders = orderService.lambdaQuery()
                .eq(Order::getStatus, OrderStatusConstants.PAID)
                .isNotNull(Order::getExpireTime)
                .le(Order::getExpireTime, now)     // 已经过了有效期
                .list();

        if (CollUtil.isEmpty(orders)) {
            log.info("没有查到需要处理的已过期订单");
            return ReturnT.SUCCESS;
        }

        log.info("查到 {} 个已过期订单，开始进行过期处理", orders.size());
        int processedCount = 0;

        for (Order order : orders) {
            try {
                // 2. 将订单状态修改为“已过期”
                order.setStatus(OrderStatusConstants.EXPIRED);
                orderService.updateById(order);

                // 3. (可选业务流转) 触发自动退款逻辑 / 回收操作
                // 真实环境需要接入微信/支付宝原路返回接口
                log.info("订单 {} 状态已变更为【已过期】，准备触发退款或补偿逻辑，原付金额 {} 分", order.getId(), order.getPayAmount());

                processedCount++;
            } catch (Exception e) {
                log.error("处理过期订单 {} 失败", order.getId(), e);
            }
        }

        log.info("======== 订单过期自动处理任务执行结束，共计处理 {} 单 ========", processedCount);
        return ReturnT.SUCCESS;
    }
}
