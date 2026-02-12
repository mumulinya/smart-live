package com.smartLive.chat.api.factory;

import com.smartLive.chat.api.RemoteChatService;
import com.smartLive.chat.api.dto.ChatMessageDTO;
import com.smartLive.chat.api.dto.SystemNoticeCreateDTO;
import com.smartLive.chat.api.dto.UserSessionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RemoteChatFallbackFactory implements FallbackFactory<RemoteChatService> {
    @Override
    public RemoteChatService create(Throwable cause) {
        return new RemoteChatService() {
            @Override
            public Long saveMessage(ChatMessageDTO chatMessage) {
                log.error("save chat message failed: {}", cause.getMessage());
                return 0L;
            }

            @Override
            public Boolean syncSession(UserSessionDTO sessionDTO) {
                log.error("sync session failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public void createSystemNotice(SystemNoticeCreateDTO noticeDTO) {
                log.error("create system notice failed: {}", cause.getMessage());
            }
        };
    }
}
