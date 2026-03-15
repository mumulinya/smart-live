package com.smartLive.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.UserAiSession;
import java.util.List;

/**
 * AI Session Service Interface
 *
 * @author smartLive
 */
public interface IUserAiSessionService extends IService<UserAiSession> {

    /**
     * Create a new session
     *
     * @param title Session title
     * @return Session ID
     */
    Long createSession(String title);

    /**
     * Get session list for current user
     *
     * @param session Query parameters
     * @return List of sessions
     */
    List<UserAiSession> selectSessionList(Integer current);
    /**
     * Search sessions by keyword
     *
     * @param userId User ID
     * @param keyword Keyword
     * @param current Page number
     * @param pageSize Page size
     * @return List of sessions
     */
    List<UserAiSession> searchByKeyword(String keyword, Integer current);

    /**
     * Delete session and associated messages
     *
     * @param sessionId Session ID
     * @return true if success
     */
    boolean deleteSession(Long sessionId);

    /**
     * Update session title
     *
     * @param sessionId Session ID
     * @param title     New title
     * @return true if success
     */
    boolean updateSessionTitle(Long sessionId, String title);
}
