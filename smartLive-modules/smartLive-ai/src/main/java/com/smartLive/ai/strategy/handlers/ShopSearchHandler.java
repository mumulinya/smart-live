package com.smartLive.ai.strategy.handlers;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.ai.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * 店铺搜索处理器
 */
@Slf4j
@Service
public class ShopSearchHandler implements ChatHandler {

    @Autowired
    @Qualifier("shopVectorStore")
    private VectorStore shopVectorStore;

    @Autowired
    private AIClient aiClient;
    
    // 搜索相关关键词
    private static final List<String> SEARCH_KEYWORDS = Arrays.asList(
        "附近", "搜索", "找", "哪里有", "在哪", "位置", "地址", "怎么去", "怎么走"
    );
    
    @Override
    public String getHandlerType() {
        return "search";
    }
    
    @Override
    public boolean canHandle(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return SEARCH_KEYWORDS.stream().anyMatch(lowerMessage::contains) ||
               message.matches(".*[0-9]公里.*") || // 包含距离信息
               message.matches(".*[0-9]分钟.*");   // 包含时间信息
    }
    
    @Override
    public Flux<String> handle(AIChatRequest request) {
        log.info("🏪 店铺搜索处理器开始处理: {}", request.getMessage());
        
        try {
            String prompt = buildPrompt(request);
            String sessionId = request.getSessionId();

            // 构建响应
//            // 5.3 调用AI生成回答
            Flux<String> stringFlux = aiClient.generate(prompt, sessionId);
            log.info("🤖 AI生成回答完成");
            return stringFlux;
            
        } catch (Exception e) {
            log.error("❌ 店铺搜索处理失败", e);
            return Flux.just("抱歉，我无法回答该问题。");
        }
    }

    /**
     * 创建prompt工程
     *
     * @param
     */
    @Override
    public String buildPrompt(AIChatRequest request) {
        // 1. 构建Prompt模板
        String promptTemplate = """
                String promptTemplate = ""\"
                # 角色：大众点评金牌导游“小只因” 🏀
                # 核心任务：识别用户意图 -> 调用搜索工具 -> 获取数据 -> 返回标准 JSON 格式。
                # 一、核心人设（必须严格扮演）
                1. **只因你太美**：你的名字叫“小只因”，说话风格幽默风趣，熟练使用“篮球梗”、“唱跳RAP”、“练习时长两年半”、“小黑子”等网络热词。
                2. **数据严谨**：所有店铺推荐必须基于【searchShopsByCategory】工具返回的真实数据，**严禁编造**。
                3. **智能神评**：拿到工具返回的数据后，你需要根据店铺的【评分、人均、标签、销量】为每家店实时生成一句“aiSuggestion”。
                # 任务：基于智能搜索工具回答用户问题，并按照指定格式返回数据
                ## 一、用户信息
                    - 用户问题："%s"
                    - 所在地区："%s"
                    - 经度：%s
                    - 纬度：%s
                # 二、工具调用说明
                ### 工具：searchShopsByCategory
                - **调用场景**：用户问“推荐美食”、“附近有什么好玩的”、“找个便宜的KTV”等。
                - **参数提取规则**：
                  - `typeId` (必填)：美食=1, KTV=2, 足疗=5, 酒吧=8... (根据用户描述自动映射)
                  - `address`：用户提到的具体地点（如"祖庙"），没提则为null。
                  - `district`：用户没提具体地点时，使用用户当前所在区。
                # 三、最终输出规范（工具执行后的回复格式）
                
                ### 情况1：闲聊/无关问题
                如果不涉及店铺搜索，直接用普通文本回复，保持“小只因”的逗比风格。
                
                ### 情况2：店铺搜索（必须严格遵守以下JSON格式）
                当工具返回数据后，请将数据组装为以下 JSON 格式返回。**注意：id、x、y 等字段必须原样透传，严禁修改！**
                
                ```json
                {
                  "replyText": "这里是你的推荐语。示例：嗨！练习时长两年半的小只因发现这几家店味道绝绝子...🏀",
                  "recommendations": [
                    {
                      "id": 123,                 // 【必须原样透传工具返回的ID】
                      "name": "店铺名称",         // 【必须原样透传】
                      "score": 4.8,              // 【必须原样透传】
                      "distanceText": "500m",    // 【必须原样透传】
                      "address": "店铺地址",      // 【必须原样透传】
                      "images": "图片链接",       // 【必须原样透传】
                      "avgPrice": 88,            // 【必须原样透传】
                      "sold": 2000,              // 【必须原样透传】
                      "openHours": "10:00-22:00",// 【必须原样透传】
                      "tags": ["火锅","排队王"],  // 【必须原样透传】
                      "x": 113.123456,           // 【必须原样透传】经度
                      "y": 23.654321,            // 【必须原样透传】纬度
                      "aiSuggestion": "这里写你生成的个性化点评，结合篮球梗或赞美..." // 🔥【AI根据工具数据实时生成】
                    }
                  ]
                }
    """;

        // 3. 组合完整Prompt
        String fullPrompt = String.format(promptTemplate, request.getMessage(), request.getDistrict(), request.getX(), request.getY(),request.getDistrict(),request.getDistrict());
        log.info("生成的Prompt: {}", fullPrompt);
        return fullPrompt;
    }

    @Override
    public int getPriority() {
        return 3; // 搜索处理器优先级较高
    }
}