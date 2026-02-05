package com.smartLive.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.Message;
import java.util.List;

/**
 * AI Message Service Interface
 *
 * @author smartLive
 */
public interface IMessageService extends IService<Message> {

    /**
     * Get message list by session ID
     *
     * @param sessionId Session ID
     * @return List of messages
     */
    List<Message> selectMessageList(Long sessionId);

    /**
     * Save a message
     *
     * @param sessionId Session ID
     * @param role Role (user/assistant)
     * @param content Content
     * @return Saved message
     */
    Message saveMessage(Long sessionId, String role, String content);
}
