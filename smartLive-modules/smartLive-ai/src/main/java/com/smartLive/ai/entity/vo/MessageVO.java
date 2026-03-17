package com.smartLive.ai.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

/**
 * 消息视图对象。
 */
@NoArgsConstructor
@Data
public class MessageVO {
    private String role;
    private String content;

    /**
     * 构造消息。
     */
    public MessageVO(Message message) {
        switch (message.getMessageType()) {
            case USER:
                role = "user";
                break;
            case ASSISTANT:
                role = "assistant";
                break;
            default:
                role = "";
                break;
        }
        this.content = message.getText();
    }
}
