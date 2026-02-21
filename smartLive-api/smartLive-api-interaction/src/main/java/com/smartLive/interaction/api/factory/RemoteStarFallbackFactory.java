package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.interaction.api.RemoteStarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteStarFallbackFactory implements FallbackFactory<RemoteStarService> {
    @Override
    public RemoteStarService create(Throwable cause) {
        return new RemoteStarService() {
            @Override
            public Boolean isStar(StarDTO starDTO) {
                log.error("isStar failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public Integer getStarCount(StarDTO starDTO) {
                log.error("getStarCount failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer getFanCount(StarDTO starDTO) {
                log.error("getFanCount failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer getCommonStarCount(StarDTO starDTO) {
                log.error("getCommonStarCount failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer getUserStarCount(StarDTO starDTO) {
                log.error("getUserStarCount failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public List<?> queryStarUserList(StarDTO starDTO) {
                log.error("queryStarUserList failed: {}", cause.getMessage());
                return List.of();
            }

            @Override
            public Map<Long, Boolean> getIsStarBatch(StarDTO starDTO, List<Long> sourceIds) {
                log.error("getIsStarBatch failed: {}", cause.getMessage());
                return Map.of();
            }
        };
    }
}
