package com.smartLive.ai.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.UserAiMessage;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 用户 AI 消息服务接口。
 *
 * 作者：smartLive
 */
public interface IUserAiMessageService extends IService<UserAiMessage> {

    /**
     * 根据会话 ID 获取消息列表。
     *
     * @param current 当前页码
     * @param sessionId 会话 ID。
     * @return 消息列表
     */
    List<UserAiMessage> selectMessageList(Integer current,Long sessionId);

    /**
     * 保存消息。
     *
     * @param sessionId 会话 ID。
     * @param role 角色（user / assistant）
     * @param content 消息内容
     * @return 已保存的消息
     */
    UserAiMessage saveMessage(Long sessionId, String role, String content);
    /**
     * 处理 AI 聊天请求。
     *
     * @param messageDTO 聊天请求参数
     * @return 聊天响应事件流
     */
    Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO);
}
