package com.smartLive.audit.chain;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审核文本提取器
 * 将不同业务快照中的关键文本统一抽取为待审核字符串
 */
@Component
public class AuditContentExtractor {

    /**
     * 从审核内容中提取文本
     *
     * @param map 审核快照
     * @return 可用于规则检测的文本
     */
    public String extractText(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 1. 标题（博客/文章/店铺活动）
        appendIfExists(sb, map, "title", true);

        // 2. 正文（博客/评论）
        appendIfExists(sb, map, "content", false);

        // 3. 用户信息（昵称/简介）
        appendIfExists(sb, map, "nickname", false);
        appendIfExists(sb, map, "introduce", false);

        // 4. 商品或活动说明
        appendIfExists(sb, map, "subTitle", false);
        appendIfExists(sb, map, "rules", false);

        // 5. 名称字段（店铺/商品）
        appendIfExists(sb, map, "name", false);
        appendIfExists(sb, map, "targetTitle", true);

        return sb.toString();
    }

    /**
     * 按字段名追加文本，避免空值干扰
     */
    private void appendIfExists(StringBuilder sb, Map<String, Object> map, String key, boolean addLineBreak) {
        Object value = map.get(key);
        if (value == null) {
            return;
        }
        sb.append(value);
        if (addLineBreak) {
            sb.append('\n');
        }
    }
}
