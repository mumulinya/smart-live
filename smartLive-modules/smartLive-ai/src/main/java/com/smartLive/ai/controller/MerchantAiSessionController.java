package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.CreateSessionDTO;
import com.smartLive.ai.domain.DTO.UpdateSessionTitleDTO;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant/session")
public class MerchantAiSessionController extends BaseController {

    @Autowired
    private IAiMerchantSessionService sessionService;

    @PostMapping
    public AjaxResult create(@RequestBody CreateSessionDTO dto) {
        Long userId = SecurityUtils.getUserId();
        dto.setUserId(userId);
        return AjaxResult.success(sessionService.createSession(userId, dto.getShopId()));
    }

    @GetMapping("/list")
    public AjaxResult list(@RequestParam("shopId") Long shopId) {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(sessionService.listByUserAndShop(userId, shopId));
    }

    @PutMapping("/{sessionId}/title")
    public AjaxResult updateTitle(@PathVariable("sessionId") Long sessionId,
                                  @RequestBody UpdateSessionTitleDTO dto) {
        Long userId = SecurityUtils.getUserId();
        sessionService.updateTitle(userId, sessionId, dto == null ? null : dto.getTitle());
        return AjaxResult.success();
    }

    @DeleteMapping("/{sessionId}")
    public AjaxResult delete(@PathVariable("sessionId") Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        sessionService.deleteSession(userId, sessionId);
        return AjaxResult.success();
    }

    @GetMapping("/{sessionId}/messages")
    public AjaxResult messages(@PathVariable("sessionId") Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(sessionService.getMessages(userId, sessionId));
    }
}
