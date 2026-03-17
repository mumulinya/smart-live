package com.smartLive.ai.api;

import com.smartLive.ai.api.DTO.AuditCheckDTO;
import com.smartLive.ai.api.DTO.AuditResultDTO;
import com.smartLive.ai.api.factory.RemoteAiAuditFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        contextId = "remoteAiAuditService",
        value = ServiceNameConstants.AI_SERVICE,
        fallbackFactory = RemoteAiAuditFallbackFactory.class
)
public interface RemoteAiAuditService {

    @PostMapping("/ai/audit/check")
    AuditResultDTO check(@RequestBody AuditCheckDTO dto);
}
