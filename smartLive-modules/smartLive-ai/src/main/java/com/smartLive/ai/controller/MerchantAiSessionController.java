package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.CreateSessionDTO;
import com.smartLive.ai.domain.DTO.UpdateSessionTitleDTO;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家 AI 会话控制器。
 */
@RestController
@RequestMapping("/merchant/session")
public class MerchantAiSessionController extends BaseController {

    @Autowired
    private IMerchantAiSessionService sessionService;

    /**
     * 创建 AjaxResult 响应。
     */
    @PostMapping
    public AjaxResult create(@RequestBody CreateSessionDTO dto) {
        Long userId = SecurityUtils.getUserId();
        dto.setUserId(userId);
        return AjaxResult.success(sessionService.createSession(userId, dto.getShopId(), dto.getType()));
    }

    /**
     * 查询 AjaxResult 响应。
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam("shopId") Long shopId,
                           @RequestParam("type") String type) {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(sessionService.listByUserAndShop(userId, shopId, type));
    }

    /**
     * 更新标题。
     */
    @PutMapping("/{sessionId}/title")
    public AjaxResult updateTitle(@PathVariable("sessionId") Long sessionId,
                                  @RequestBody UpdateSessionTitleDTO dto) {
        Long userId = SecurityUtils.getUserId();
        sessionService.updateTitle(userId, sessionId, dto == null ? null : dto.getTitle());
        return AjaxResult.success();
    }

    /**
     * 删除 AjaxResult 响应。
     */
    @DeleteMapping("/{sessionId}")
    public AjaxResult delete(@PathVariable("sessionId") Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        sessionService.deleteSession(userId, sessionId);
        return AjaxResult.success();
    }

    /**
     * 获取 AjaxResult 响应。
     */
    @GetMapping("/{sessionId}/messages")
    public AjaxResult messages(@PathVariable("sessionId") Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(sessionService.getMessages(userId, sessionId));
    }
}