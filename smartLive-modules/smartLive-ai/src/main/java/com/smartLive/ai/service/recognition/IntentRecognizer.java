package com.smartLive.ai.service.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 意图识别服务，负责根据用户输入识别出对应的意图
 */
@Slf4j
@Service
public class IntentRecognizer {
    
    // 意图关键词映射
    private static final List<IntentKeyword> INTENT_KEYWORDS = Arrays.asList(
            new IntentKeyword("comment", Arrays.asList("好评","差评","追评","晒单","反馈","体验感","性价比","推荐指数","踩雷","种草","真实评价","细节点评","优惠实测","划算","不划算","避雷提醒","复购意愿","服务评价","口味评价","环境评价","活动体验","评价","评论")),
        new IntentKeyword("voucher", Arrays.asList("优惠","券","折扣","打折","活动","促销","团购","套餐","省钱","便宜","特价","秒杀","立减","满减")),
        new IntentKeyword("shopSearch", Arrays.asList("店铺","餐厅","饭店","营业时间","地址","位置","附近","周边","推荐","评分","评价")));

    public String recognizeIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "unknown";
        }
        
        String lowerMessage = message.toLowerCase();
        
        // 1. 规则匹配
        String intent = ruleBasedRecognize(lowerMessage);
        if (!"unknown".equals(intent)) {
            return intent;
        }
        
        // 2. 简单的AI规则匹配（后续可以接入真正的AI）
        return aiBasedRecognize(lowerMessage);
    }

    /**
     * 基于规则的意图识别
     */
    private String ruleBasedRecognize(String message) {
        for (IntentKeyword keyword : INTENT_KEYWORDS) {
            for (String kw : keyword.keywords) {
                if (message.contains(kw)) {
                    log.info("📏 规则匹配: {} -> {}", kw, keyword.intent);
                    return keyword.intent;
                }
            }
        }
        return "unknown";
    }
    
    private String aiBasedRecognize(String message) {
        // 简单的长度和内容判断（后续替换为真正的AI调用）
        if (message.length() > 20) {
            return "search"; // 长文本倾向于搜索
        } else if (message.contains("?") || message.contains("？")) {
            return "service"; // 问句倾向于客服
        } else {
            return "recommend"; // 短文本倾向于推荐
        }
    }
    
    // 内部关键词类
    private static class IntentKeyword {
        String intent;
        List<String> keywords;
        
        IntentKeyword(String intent, List<String> keywords) {
            this.intent = intent;
            this.keywords = keywords;
        }
    }
}