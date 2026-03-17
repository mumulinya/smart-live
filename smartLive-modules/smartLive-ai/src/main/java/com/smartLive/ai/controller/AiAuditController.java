package com.smartLive.ai.controller;

import cn.hutool.json.JSONUtil;
import com.smartLive.ai.api.DTO.AuditCheckDTO;
import com.smartLive.ai.api.DTO.AuditResultDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 审核控制器。
 */
@RestController
@RequestMapping("/ai/audit")
public class AiAuditController {

    private final ChatClient chatClient;

    /**
     * 构造 AI 审核控制器。
     */
    public AiAuditController(@Qualifier("auditChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 校验审核结果。
     */
    @PostMapping("/check")
    public AuditResultDTO check(@RequestBody AuditCheckDTO dto) {
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            return new AuditResultDTO(null, "审核内容为空");
        }
        String auditType = normalizeType(dto.getType());
        String prompt = """
                请审核以下%s内容是否合规。
                请重点判断是否包含广告引流、色情低俗、暴力血腥、辱骂攻击、违法违规等内容。
                只返回 JSON，不要返回 markdown，不要解释。
                返回格式固定为：{"pass":true/false,"reason":"拒绝原因"}
                通过时 reason 返回空字符串。

                内容如下：
                %s
                """.formatted(resolveTypeDesc(auditType), dto.getContent().trim());

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return parseAuditResult(result);
    }

    /**
     * 解析审核结果。
     */
    private AuditResultDTO parseAuditResult(String result) {
        if (result == null || result.isBlank()) {
            return new AuditResultDTO(null, "AI审核结果为空");
        }
        String cleaned = result.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "")
                    .replace("```JSON", "")
                    .replace("```", "")
                    .trim();
        }
        try {
            AuditResultDTO auditResult = JSONUtil.toBean(cleaned, AuditResultDTO.class);
            if (auditResult == null || auditResult.getPass() == null) {
                return new AuditResultDTO(null, "AI审核结果缺少 pass 字段");
            }
            if (Boolean.TRUE.equals(auditResult.getPass())) {
                auditResult.setReason("");
            } else if (auditResult.getReason() == null || auditResult.getReason().isBlank()) {
                auditResult.setReason("内容包含违规信息");
            }
            return auditResult;
        } catch (Exception ex) {
            return new AuditResultDTO(null, "AI审核结果解析失败");
        }
    }

    /**
     * 规范化类型。
     */
    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "content";
        }
        return type.trim().toLowerCase();
    }

    /**
     * 解析类型描述。
     */
    private String resolveTypeDesc(String type) {
        return switch (type) {
            case "blog" -> "博客";
            case "review" -> "评价";
            default -> "内容";
        };
    }
}
