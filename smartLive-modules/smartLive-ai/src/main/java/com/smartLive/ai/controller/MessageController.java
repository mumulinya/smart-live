package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 娑堟伅绠＄悊 Controller
 * 澶勭悊鐢ㄦ埛涓?AI 鐨勫疄鏃跺璇濓紙鍩轰簬 SSE 娴佸紡鍝嶅簲锛変互鍙婂巻鍙叉秷鎭煡璇?
 *
 * @author smartLive
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    /**
     * 鑾峰彇鍘嗗彶娑堟伅鍒楄〃
     * 鍒嗛〉鏌ヨ鎸囧畾浼氳瘽鐨勫巻鍙茶亰澶╄褰?
     *
     * @param current 褰撳墠椤电爜
     * @param sessionId 浼氳瘽 ID
     * @return 鍖呭惈娑堟伅瀹炰綋鍒楄〃鐨?Result
     */
    @GetMapping("/list")
    public Result getMessageList(@RequestParam("current") Integer current, @RequestParam("sessionId") Long sessionId) {
        List<Message> list = messageService.selectMessageList(current, sessionId);
        return Result.ok(list);
    }

    /**
     * AI 鏅鸿兘瀵硅瘽鎺ュ彛
     * 閲囩敤 Server-Sent Events (SSE) 鎶€鏈疄鐜版祦寮忔枃鏈緭鍑猴紝鎻愪緵娴佺晠鐨勬墦瀛楁満浜や簰浣撻獙
     *
     * @param messageDTO 鍖呭惈鐢ㄦ埛娑堟伅銆佷細璇濅笂涓嬫枃鍙婂湴鐞嗕綅缃瓑淇℃伅鐨?DTO
     * @return SSE 娴侊紝鏁版嵁浜嬩欢鍚嶄负 "message"锛堟鏂囧垎鐗囷級鎴?"card_render"锛堟帹鑽愬崱鐗囷級
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        return messageService.chat(messageDTO);
    }
}
