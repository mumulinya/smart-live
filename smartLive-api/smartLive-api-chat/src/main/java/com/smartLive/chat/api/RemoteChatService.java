package com.smartLive.chat.api;
import com.smartLive.chat.api.dto.ChatMessageDTO;
import com.smartLive.chat.api.dto.UserSessionDTO;
import com.smartLive.chat.api.factory.RemoteChatFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "remoteChatService", value = ServiceNameConstants.CHAT_SERVICE,fallbackFactory = RemoteChatFallbackFactory.class)
public interface RemoteChatService {

    @PostMapping("/inner/chat/msg")
    Long saveMessage(@RequestBody ChatMessageDTO chatMessage);

    @PostMapping("/inner/chat/session")
    Boolean syncSession(@RequestBody UserSessionDTO sessionDTO);
}
