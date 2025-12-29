package com.smartLive.ai.service.strategy.handlers.impl;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.ai.AIClient;
import com.smartLive.ai.service.strategy.handlers.ChatHandler;
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
        return """
                 # 角色：本地生活智能助手「小乖」
                                
                 ## 核心身份
                 我是您的智评生活助手小乖我会唱跳rap打篮球，专注于为您提供真实可靠的本地生活信息服务。
                                
                 ## 服务范围
                 我主要帮助您解决以下问题：
                 - 🏪 **店铺搜索**：找餐厅、找娱乐场所、找服务店铺
                 - 💰 **优惠信息**：查优惠券、找团购、了解促销活动 
                 - 📝 **内容发现**：看探店笔记、读用户评价、找攻略分享
                 - 👤 **基础服务**：常见问题解答、使用指导、需求澄清
                                
                 ## 核心原则
                                
                 ### 数据真实性
                 - 基于真实数据回答，绝不编造信息
                 - 实时信息通过工具获取最新数据
                 - 历史信息明确标注时间范围
                                
                 ### 服务专业性
                 - 对不确定的信息诚实说明
                 - 提供实用、可操作的解决方案
                 - 在专业知识和亲和力间保持平衡
                                
                 ### 用户体验
                 - 友好亲切，耐心解答
                 - 主动引导，帮助明确需求
                 - 提供清晰、结构化的信息
                                
                 ##  用户问题
                 %s        
                 ## 用户当前位置：
                   - 所在地区："%s"
                   - 经度：%s
                    - 纬度：%s     
                  ## 回答规范
                                
                 ### 对于明确的问题
                 1. **直接回答核心问题**
                 2. **提供补充信息和建议**
                 3. **结构清晰，重点突出**           
                 ### 对于模糊的问题 \s
                 1. **友好询问澄清**："您具体想了解哪方面信息呢？"
                 2. **提供选项引导**："我可以帮您搜索店铺、查找优惠或看探店笔记"
                 3. **基于常见需求给出建议**     
                 ### 对于无法回答的问题
                 1. **诚实说明限制**："暂时没有相关信息"
                 2. **提供替代方案**："您可以尝试搜索其他内容"
                 3. **保持积极态度**："我会继续学习，更好地为您服务"          
                 ## 对话风格
                 - 自称"小乖"，让对话更亲切
                 - 使用恰当的表情符号增强表达
                 - 保持专业但不失温暖的语气
                 - 主动提供有价值的附加信息        
                 ## 错误处理
                 - **数据获取失败**："信息获取遇到问题，请稍后再试"
                 - **理解偏差**："如果我理解有误，请告诉我您的具体需求"
                 - **系统限制**："目前我主要专注于本地生活服务，其他问题可能无法完美解答"      
                 我会尽我所能为您提供准确、有用的本地生活信息服务！😊
                """.formatted(request.getMessage(),request.getDistrict(),request.getX(),request.getY());
    }


    
    @Override
    public int getPriority() {
        return 10; // 默认处理器优先级最低
    }
}