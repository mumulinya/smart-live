package com.smartlive.im.feign;

import com.smartLive.common.core.domain.R;
import com.smartlive.im.dto.ChatMessageDTO;
import com.smartlive.im.dto.UserSessionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "remoteChatService", value = "smartLive-chat")
public interface RemoteChatService {

    @PostMapping("/inner/chat/msg")
    R<Boolean> saveMessage(@RequestBody ChatMessageDTO chatMessage);

    @PostMapping("/inner/chat/session")
    R<Boolean> syncSession(@RequestBody UserSessionDTO sessionDTO);
}
