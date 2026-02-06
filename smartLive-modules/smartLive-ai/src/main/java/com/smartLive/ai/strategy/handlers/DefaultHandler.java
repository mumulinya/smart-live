package com.smartLive.ai.strategy.handlers;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.ai.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class DefaultHandler implements ChatHandler {
    
    @Autowired
    private AIClient aiClient;
    
    @Override
    public String getHandlerType() {
        return "default";
    }
    
    @Override
    public boolean canHandle(String message) {
        return true; // 默认处理器可以处理所有消息
    }
    
    @Override
    public Flux<String> handle(AIChatRequest request) {
        log.info("🔧 默认处理器处理消息: {}", request.getMessage());
        
        try {
            // 直接调用AI进行通用对话
            String prompt = buildPrompt(request);
            String sessionId = request.getSessionId();
            Flux<String> aiResponse = aiClient.generate(prompt,sessionId);
            
            return aiResponse;
                    
        } catch (Exception e) {
            log.error("❌ 默认处理器处理失败", e);
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
        return
                """                                                                                                                
     # 角色
     你是「小只因」不仅会回答用户需求，还会唱 跳 rap 篮球，智评生活本地生活助手。
     # 能力范围
     | 类型 | 说明 |
     |------|------|
     | 🏪 店铺搜索 | 餐厅、娱乐、服务店铺推荐 |
     | 💰 优惠查询 | 优惠券、团购、促销活动 |
     | 📝 内容发现 | 探店笔记、用户评价、攻略 |
     | 👤 基础服务 | 使用指导、需求澄清 |

     # 用户信息
     - 问题："%s"
     - 位置：%s（经度：%s，纬度：%s）

     # 核心原则
     1. **真实性**：数据来自工具调用，不编造
     2. **实用性**：给出可操作的具体建议
     3. **简洁性**：重点突出，避免冗长

     # 回答策略

     ## 需求明确时
     - 直接回答 + 补充建议
     - 结构化展示信息
     - 主动引导下一步操作

     ## 需求模糊时
     - 友好询问：「您想找店铺、查优惠还是看笔记？」
     - 给出 2-3 个选项引导

     ## 无法回答时
     - 诚实说明：「暂无相关信息」
     - 提供替代方案

     # 输出风格
     - 自称「小只因」
     - 适度使用 emoji，不过度
     - 亲切专业，简洁有力
     - 每次回答控制在合理长度

     # 异常处理
     - 数据获取失败：「信息获取遇到问题，请稍后再试」
     - 超出范围：「我专注本地生活服务，这个问题可能帮不上忙」
     """.formatted(request.getMessage(),request.getDistrict(),request.getX(),request.getY());
    }



    @Override
    public int getPriority() {
        return 10; // 默认处理器优先级最低
    }
}