package com.smartLive.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.Session;
import java.util.List;

/**
 * AI Session Service Interface
 *
 * @author smartLive
 */
public interface ISessionService extends IService<Session> {

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
    List<Session> selectSessionList(Integer current);
    /**
     * Search sessions by keyword
     *
     * @param userId User ID
     * @param keyword Keyword
     * @param current Page number
     * @param pageSize Page size
     * @return List of sessions
     */
    List<Session> searchByKeyword(String keyword, Integer current);
}
