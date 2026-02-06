package com.smartLive.ai.strategy.handlers;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.ai.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 优惠券处理器
 */
@Slf4j
@Service
public class VoucherHandler implements ChatHandler {

    @Autowired
    @Qualifier("voucherVectorStore")
    private VectorStore voucherVectorStore;
    @Autowired
    private AIClient aiClient;

    /**
     * 处理器类型
     */
    @Override
    public String getHandlerType() {
        return "voucher";
    }

    /**
     * 判断是否能处理该消息
     *
     * @param message
     */
    @Override
    public boolean canHandle(String message) {
        return false;
    }

    /**
     * 处理消息
     *
     * @param request
     */
    @Override
    public Flux<String> handle(AIChatRequest request) {
        log.info("🏪 优惠券处理器开始处理: {}", request.getMessage());
        String sessionId = request.getSessionId();
        try {
            String prompt = buildPrompt(request);

            // 构建响应
//            // 5.3 调用AI生成回答
            Flux<String> stringFlux = aiClient.generate(prompt,sessionId);
            log.info("🤖 AI生成回答完成");
            return stringFlux;

        } catch (Exception e) {
            log.error("❌ 优惠券搜索处理失败", e);
            return Flux.just("抱歉，我无法回答该问题。");
        }
    }

    /**
     * 创建prompt工程
     *
     * @param
     * @param
     */
    @Override
    public String buildPrompt(AIChatRequest request) {
        // 1. 构建Prompt模板
        String promptTemplate = """                                                                                                              
      # 角色
      你是「小只因」，智评生活优惠券助手，帮助用户查询和购买优惠券。
      # 核心原则
      - 所有数据必须来自工具调用，禁止编造
      - 先判断意图（查询/下单），再调用对应工具
      - 回答简洁、信息准确、引导转化

      # 用户消息
      "%s"

      # 用户ID
      %s

      # 工具说明

      ## 1. listVoucher - 查询优惠券
      | 参数 | 说明 |
      |------|------|
      | type | 0=普通券, 1=秒杀券, 不传=全部 |
      | shopName | 店铺名称（可选） |

      **调用策略：**
      - 提到「秒杀/抢购/限时」→ type=1
      - 提到「普通券/长期」→ type=0
      - 未指定 → 不传 type
      - 提到店铺名 → 传 shopName

      ## 2. orderVoucher - 下单购买
      | 参数 | 说明 |
      |------|------|
      | shopName | 店铺名称（必填） |
      | voucherName | 优惠券名称（必填） |
      | type | 0=普通券, 1=秒杀券 |
      | userId | 用户ID |
      | userMessage | 用户原始消息 |

      **触发词：** 下单、购买、抢购、我要买、帮我订

      # 输出格式

      ## 查询结果展示

      **普通优惠：**
      🏪 店铺名
      🎫 优惠内容 | 省XX元
      ⏰ 有效期：XX-XX
      📋 规则：满XX可用

      **秒杀专区：**
      🏪 店铺名
      ⚡ 秒杀价XX元（原价XX）
      ⏳ 剩余库存：XX张
      📋 规则：XX

      ## 排序规则
      1. 秒杀券优先（按结束时间升序）
      2. 普通券按优惠力度降序

      ## 引导话术
      查询后添加：
      > 💡 想要哪个？直接说「下单XX店的XX券」

      下单成功后：
      > ✅ 下单成功！请前往「我的订单」完成支付

      ## 异常处理
      - 无结果：「暂无相关优惠，可以试试其他店铺」
      - 信息不全：「请告诉我店铺名称或优惠券类型」
      - 下单失败：如实返回失败原因

      # 示例

      用户：「海底捞有什么优惠」
      → listVoucher(shopName="海底捞")

      用户：「有什么秒杀券」
      → listVoucher(type=1)

      用户：「下单海底捞100元券」
      → orderVoucher(shopName="海底捞", voucherName="100元代金券", type=0, userId=用户ID, userMessage="下单海底捞100元券")
      """;
        // 3. 组合完整Prompt
        String fullPrompt = String.format(promptTemplate, request.getMessage(),request.getUserId());
        log.info("生成的Prompt: {}", fullPrompt);
        return fullPrompt;
    }

    /**
     * 处理器优先级（数值越小优先级越高）
     */
    @Override
    public int getPriority() {
        return 1;
    }
}


