package com.smartLive.ai.strategy.handlers.impl;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.service.ai.AIClient;
import com.smartLive.ai.service.rag.impl.CommentRagService;
import com.smartLive.ai.strategy.handlers.ChatHandler;
import com.smartLive.interaction.api.RemoteCommentService;
import com.smartLive.interaction.api.dto.CommentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 评论处理器
 */
@Slf4j
@Service
public class CommentHandler implements ChatHandler {
    @Autowired
    private CommentRagService commentRagService;

    @Autowired
    private AIClient aiClient;

    @Autowired
    private RemoteCommentService remoteCommentService;

    /**
     * 处理器类型
     */
    @Override
    public String getHandlerType() {
        return "comment";
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
        log.info("🏪 评论处理器开始处理: {}", request.getMessage());

        try {
            String prompt = buildPrompt(request);
            String sessionId = request.getSessionId();

            // 构建响应
//            // 5.3 调用AI生成回答
            Flux<String> stringFlux = aiClient.generate(prompt,sessionId);
            log.info("🤖 AI生成回答完成");
            return stringFlux;

        } catch (Exception e) {
            log.error("❌ 评论处理器处理失败", e);
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
                 String prompt = ""\"
                 # 角色：大众点评评论专家「小乖」
                   
                 # 任务：基于智能评论工具回答用户评论相关问题，工具内部自动结合RAG和数据库查询
                   
                 # 核心原则：
                 1. **统一评论入口**：所有评论相关问题都通过getComments工具处理
                 2. **智能数据融合**：工具内部自动结合RAG向量搜索，返回真实评论数据
                 3. **评论质量优先**：筛选高质量、有参考价值的评论展示
                 4. **多源评论整合**：支持店铺、文章、团购等多种来源的评论
                   
                 ## 一、用户搜索需求
                 "%s"
                   
                 ## 二、评论工具说明
                 ### 工具：getComments（智能评论查询）
                 - **功能**：统一评论查询入口，内部自动执行：
                   1. RAG向量搜索：基于语义相似度查找相关评论
                   2. 数据库查询：确保返回完整、实时的评论信息
                   3. 质量筛选：优先展示高质量、有帮助的评论
                   
                 - **参数说明**：
                   - **sourceType（必传）**：评论来源类型
                     - 1：博客评论（对应blogId）
                     - 2：店铺评论（对应shopId）\s
                     - 3：团购评论（对应dealId）
                   - **sourceId（必传）**：来源ID，根据sourceType对应不同表的主键
                   
                 ## 三、智能参数提取与调用策略
                   
                 ### 1. 调用场景识别：
                 - ✅ **店铺评论**：用户询问具体店铺的评价、口碑、用户体验
                   - "海底捞的评价怎么样"
                   - "XX酒店的用户反馈"
                   - "这家餐厅的口碑如何"
                  \s
                 - ✅ **文章评论**：用户询问某篇文章的读者反馈
                   - "那篇美食攻略的评论"
                   - "旅游文章的读者反馈"
                  \s
                 - ✅ **团购评论**：用户询问团购产品的用户体验
                   - "这个团购套餐的评价"
                   - "优惠活动的用户反馈"
                   
                 ### 2. 参数自动映射：
                - **识别文章**：用户提到具体文章 → sourceType=1, sourceId=博客ID \s
                 - **识别店铺**：从上下文或用户问题中提取店铺ID/名称 → sourceType=2, sourceId=店铺ID
                 - **识别团购**：用户提到团购产品 → sourceType=3, sourceId=团购ID
                   
                 ## 四、评论展示规范
                   
                 ### 1. 数据来源说明：
                 - 开头统一说明："基于真实用户评论为您提供参考"
                   
                 ### 2. 结构化输出：
                 - **评论排序**：按点赞数、评论质量、时间综合排序
                 - **评论信息包含**：
                   👤 用户昵称（匿名处理）
                   ⭐ 评分（如有）
                   📝 评论内容
                   👍 有用数
                   📅 评论时间
                   💡 评论标签（如"环境好"、"服务赞"、"性价比高"等）
                   
                 ### 3. 输出格式：
                 📊 评论概览：共X条评论，平均评分X.X
                   
                 👤 用户A [匿名]
                 ⭐ 评分：5.0
                 📝 非常满意的体验，环境优雅，服务周到
                 👍 25人觉得有用 | 📅 2024-01-15
                 💡 标签：环境优雅、服务赞
                   
                 👤 用户B [匿名]
                 ⭐ 评分：4.0
                 📝 菜品味道不错，就是等待时间稍长
                 👍 12人觉得有用 | 📅 2024-01-10
                 💡 标签：味道好、等待时间
                   
                 text
                   
                 ### 4. 评论分析总结：
                 - **好评重点**：总结用户普遍称赞的方面
                 - **改进建议**：提及用户反馈的改进点（如有）
                 - **消费提示**：基于评论给出实用建议
                   
                 ### 5. 无评论处理：
                 - "暂未找到相关评论"
                 - "建议您查看其他类似店铺的评论参考"
                   
                 ## 五、与店铺搜索的协同工作
                   
                 ### 场景1：先搜店铺，再查评论
                 用户："推荐几家好吃的川菜馆" → 调用searchShopsByCategory
                 用户："那家XX川菜馆的评价怎么样" 
                 1.先调用getShopDetails工具  name=xx 获取店铺Id
                 2.再调用getComments(sourceType=2, sourceId=店铺Id)
                   
                 ### 场景2：直接查询评论
                 用户："海底捞的评论怎么样" → 先识别店铺ID，再调用getComments(sourceType=2, sourceName=海底捞)
                 
                 当用户询问店铺评价时：
                  1. **先调用店铺搜索**：使用 searchShopsByCategory 找到目标店铺
                  2. **记录店铺ID**：从搜索结果中获取准确的 shopId映射为sourceId
                  3. **再调用评论查询**：使用 getShopComments(sourceType,sourceId, sourceName)
                  
                  ### 示例流程：
                  用户："海底捞的评价怎么样？"
                  → 步骤1：searchShopsByCategory("海底捞") → 返回店铺ID: 123
                  → 步骤2：getShopComments(sourceType=2,sourceId=123, sourceName="海底捞")
                   
                 ## 六、执行流程
                 1. **意图识别**：分析用户是询问店铺/文章/团购的评论
                 2. **来源识别**：确定sourceType和对应的sourceId
                 3. **调用工具**：自动调用getComments工具
                 4. **评论分析**：基于真实评论数据生成总结和建议
                   
                 ## 七、注意事项
                 - 保护用户隐私，匿名化处理用户信息
                 - 客观呈现好评和差评，不偏颇
                 - 如评论数量少，如实告知用户
                 - 基于真实数据，不编造评论内容
                   
                 请严格按照以上流程执行，基于智能评论工具为用户提供真实、有价值的评论参考：                
                 """;

        // 3. 组合完整Prompt
        String fullPrompt = String.format(promptTemplate, request.getMessage());
        log.info("生成的Prompt: {}", fullPrompt);
        return fullPrompt;
    }

    /**
     * 处理器优先级（数值越小优先级越高）
     */
    @Override
    public int getPriority() {
        return 2;
    }


    /**
     * ai生成评论
     *
     * @param
     * @return
     */
    public void aiCreateComment(List<AIGenerateRequest> list) {
        List<CommentDTO> comments = new ArrayList<>();
        list.forEach(request -> {
            request.getSourceIds().forEach(sourceId -> {
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setSourceType(Integer.valueOf(request.getSourceType()));
                commentDTO.setSourceId(sourceId);
                CommentDTO comment = createComment(commentDTO);
                comments.add(comment);
            });
        });
        if (comments.isEmpty()){
            return;
        }
        log.info("要生成的评价为，参数：{}", comments);
        // 保存
        remoteCommentService.saveAiCreateComment(comments);
    }
    /**
     * 创建评论
     *
     * @param commentDTO
     */
    private CommentDTO createComment(CommentDTO commentDTO){
        // 获取评论
        List<CommentDTO> comments = commentRagService.getComments(commentDTO, null);
        if (comments == null || comments.isEmpty()) {
            return null;
        }
        String prompt = buildSummaryPrompt(comments);
        String context = aiClient.firstGenerate(prompt);
        commentDTO.setContent(context);
        commentDTO.setRating(5);
        commentDTO.setCreateTime(new Date());
        commentDTO.setStatus(1);
        commentDTO.setUserId(99999L);
        commentDTO.setParentId(0L);
        commentDTO.setIsAIGenerated(true);
        log.info("生成的评论为：{}", commentDTO);
        return commentDTO;
    }

    /**
     * 构建总结分析的Prompt
     *
     * @param
     * @param
     * @return
     */
    public String buildSummaryPrompt(List<CommentDTO> comments) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("请用一段话总结以下用户评价的核心内容，要求：\n");
        promptBuilder.append("1. 概括主要评价倾向（正面/负面/中性）\n");
        promptBuilder.append("2. 提炼用户最关注的2-3个方面\n");
        promptBuilder.append("3. 语言简洁明了，控制在100字以内\n\n");

        promptBuilder.append("用户评价：\n");

        if (comments == null || comments.isEmpty()) {
            promptBuilder.append("暂无评价数据");
        } else {
            for (int i = 0; i < comments.size(); i++) {
                CommentDTO comment = comments.get(i);
                promptBuilder.append(i + 1).append(". ");

                if (comment.getContent() != null) {
                    promptBuilder.append(comment.getContent());
                }

                if (comment.getRating() != null) {
                    promptBuilder.append(" [").append(comment.getRating()).append("星]");
                }

                promptBuilder.append("\n");
            }
        }

        promptBuilder.append("\n请用一段话总结：");

        return promptBuilder.toString();
    }
}