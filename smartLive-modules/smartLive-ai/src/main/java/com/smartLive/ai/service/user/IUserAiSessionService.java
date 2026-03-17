package com.smartLive.ai.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.UserAiSession;
import java.util.List;

/**
 * 用户 AI 会话服务接口。
 *
 * 作者：smartLive
 */
public interface IUserAiSessionService extends IService<UserAiSession> {

    /**
     * 创建新会话。
     *
     * @param title 会话标题
     * @return 会话 ID。
     */
    Long createSession(String title);

    /**
     * 获取当前用户的会话列表。
     *
     * @param current 当前页码
     * @return 会话列表
     */
    List<UserAiSession> selectSessionList(Integer current);
    /**
     * 按关键词搜索会话。
     *
     * @param keyword 搜索关键词
     * @param current 当前页码
     * @return 搜索到的会话列表
     */
    List<UserAiSession> searchByKeyword(String keyword, Integer current);

    /**
     * 删除会话及其关联消息。
     *
     * @param sessionId 会话 ID。
     * @return 是否删除成功
     */
    boolean deleteSession(Long sessionId);

    /**
     * 更新会话标题。
     *
     * @param sessionId 会话 ID。
     * @param title 新标题
     * @return 是否更新成功
     */
    boolean updateSessionTitle(Long sessionId, String title);
}
