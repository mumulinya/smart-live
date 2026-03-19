package com.smartLive.audit.chain.handler;

import com.smartLive.ai.api.DTO.AuditCheckDTO;
import com.smartLive.ai.api.DTO.AuditResultDTO;
import com.smartLive.ai.api.RemoteAiAuditService;
import com.smartLive.audit.chain.AuditContentExtractor;
import com.smartLive.audit.chain.AuditProcessContext;
import com.smartLive.audit.chain.AuditProcessHandler;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 审核处理器
 * 当前仅对博客与评价内容接入 AI 审核 RPC
 */
@Slf4j
@Component
@Order(200)
public class AiAuditHandler implements AuditProcessHandler {

    private final RemoteAiAuditService remoteAiAuditService;
    private final AuditContentExtractor auditContentExtractor;

    public AiAuditHandler(RemoteAiAuditService remoteAiAuditService,
                          AuditContentExtractor auditContentExtractor) {
        this.remoteAiAuditService = remoteAiAuditService;
        this.auditContentExtractor = auditContentExtractor;
    }

    /**
     * 执行 AI 审核逻辑
     *
     * @param context 审核上下文
     */
    @Override
    public void handle(AuditProcessContext context) {
        if (context == null || context.isFinished() || context.getAuditMessage() == null) {
            return;
        }

        String auditType = resolveAuditType(context.getAuditMessage().getBizType());
        if (auditType == null) {
            return;
        }

        String content = auditContentExtractor.extractText(context.getAuditMessage().getAuditContent());
        if (content == null || content.isBlank()) {
            log.info("AI审核跳过，内容为空，taskId={}, bizType={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType());
            return;
        }

        AuditResultDTO auditResult;
        try {
            log.info("开始AI审核，taskId={}, bizType={}, content={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType(), content);
            auditResult = remoteAiAuditService.check(new AuditCheckDTO(content.trim(), auditType));
            log.info("AI审核完成，taskId={}, bizType={}, pass={}, reason={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType(),
                    auditResult.getPass(), auditResult.getReason());
        } catch (Exception ex) {
            log.error("AI审核RPC调用异常，taskId={}, bizType={}, error={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType(), ex.getMessage());
            return;
        }

        if (auditResult == null || auditResult.getPass() == null) {
            log.warn("AI审核未返回明确结论，taskId={}, bizType={}, reason={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType(),
                    auditResult == null ? "result is null" : auditResult.getReason());
            return;
        }

        if (Boolean.TRUE.equals(auditResult.getPass())) {
            context.waitManual("AI初审通过，待人工复核");
            log.info("AI初审通过并转人工复核，taskId={}, bizType={}",
                    context.getAuditTaskId(), context.getAuditMessage().getBizType());
            return;
        }

        String reason = auditResult.getReason() == null || auditResult.getReason().isBlank()
                ? "AI审核未通过"
                : auditResult.getReason().trim();
        context.reject(reason);
        log.info("AI审核拒绝，taskId={}, bizType={}, reason={}",
                context.getAuditTaskId(), context.getAuditMessage().getBizType(), reason);
    }

    /**
     * 将业务类型映射为 AI 审核类型
     *
     * @param bizType 业务类型编码
     * @return AI 审核类型标识
     */
    private String resolveAuditType(Integer bizType) {
        if (GlobalBizTypeEnum.BLOG.getCode().equals(bizType)) {
            return "blog";
        }
        if (GlobalBizTypeEnum.REVIEW.getCode().equals(bizType)) {
            return "review";
        }
        return null;
    }
}
