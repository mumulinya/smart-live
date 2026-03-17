package com.smartLive.ai.api.factory;

import com.smartLive.ai.api.DTO.AuditCheckDTO;
import com.smartLive.ai.api.DTO.AuditResultDTO;
import com.smartLive.ai.api.RemoteAiAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoteAiAuditFallbackFactory implements FallbackFactory<RemoteAiAuditService> {

    @Override
    public RemoteAiAuditService create(Throwable cause) {
        return new RemoteAiAuditService() {
            @Override
            public AuditResultDTO check(AuditCheckDTO dto) {
                log.error("check ai audit failed: {}", cause.getMessage());
                return new AuditResultDTO(null, "AI审核服务暂不可用");
            }
        };
    }
}
