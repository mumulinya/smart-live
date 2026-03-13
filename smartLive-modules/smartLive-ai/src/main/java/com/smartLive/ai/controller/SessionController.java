package com.smartLive.ai.controller;

import com.smartLive.ai.domain.Session;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 瀵硅瘽浼氳瘽绠＄悊 Controller
 * 璐熻矗浼氳瘽鐨勫垱寤恒€佸垪琛ㄦ煡璇€侀噸鍛藉悕鍙婂垹闄ょ瓑鐢熷懡鍛ㄦ湡绠＄悊
 *
 * @author smartLive
 */
@Slf4j
@RestController
@RequestMapping("/session")
public class SessionController extends BaseController {

    @Autowired
    private ISessionService sessionService;

    /**
     * 鍒涘缓鏂颁細璇?
     *
     * @param params 鍖呭惈浼氳瘽鏍囬鐨?Map
     * @return 鏂板垱寤虹殑浼氳瘽 ID
     */
    @PostMapping("/create")
    public Result createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        Long sessionId = sessionService.createSession(title);
        return Result.ok(sessionId);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勪細璇濆垪琛?
     * 鎸夋渶鍚庢椿璺冩椂闂村€掑簭鎺掑垪锛屾敮鎸佸垎椤?
     *
     * @param current 褰撳墠椤电爜
     * @return 浼氳瘽瀵硅薄鍒楄〃
     */
    @GetMapping("/list")
    public Result getSessionList(@RequestParam("current") Integer current) {
        List<Session> list = sessionService.selectSessionList(current);
        return Result.ok(list);
    }
    /**
     * 鎼滅储鐢ㄦ埛浼氳瘽
     */
    @GetMapping("/search")
    public Result searchSession(
            @RequestParam("keyword") String keyword,
            @RequestParam("current") Integer current) {
        List<Session> list = sessionService.searchByKeyword(keyword, current);
        return Result.ok(list);
    }

    /**
     * 鍒犻櫎鎸囧畾浼氳瘽
     * 鍚屾鍒犻櫎璇ヤ細璇濅笅鐨勬墍鏈夎亰澶╂秷鎭褰?
     *
     * @param sessionId 浼氳瘽 ID
     * @return 鎿嶄綔鎴愬姛鎴栧け璐ョ殑 Result
     */
    @DeleteMapping("/{sessionId}")
    public Result deleteSession(@PathVariable("sessionId") Long sessionId) {
        boolean success = sessionService.deleteSession(sessionId);
        if (success) {
            return Result.ok();
        }
        return Result.fail("鍒犻櫎浼氳瘽澶辫触");
    }

    /**
     * 鏇存柊浼氳瘽鏍囬锛堥噸鍛藉悕锛?
     *
     * @param sessionId 浼氳瘽 ID
     * @param params 鍖呭惈鏂?title 鐨?Map
     * @return 鎿嶄綔缁撴灉
     */
    @PutMapping("/{sessionId}/title")
    public Result updateSessionTitle(@PathVariable("sessionId") Long sessionId, @RequestBody Map<String, String> params) {
        String title = params.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.fail("鏍囬涓嶈兘涓虹┖");
        }
        boolean success = sessionService.updateSessionTitle(sessionId, title);
        if (success) {
            return Result.ok();
        }
        return Result.fail("鏇存柊鏍囬澶辫触");
    }
}
