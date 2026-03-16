package com.smartlive.chat.controller;

import java.util.List;

import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.service.IUserSessionsService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;

/**
 * 用户会话列表Controller
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@RestController
@RequestMapping("/userSession")
public class UserSessionsController extends BaseController
{
    @Autowired
    private IUserSessionsService userSessionsService;

    /**
     * 查询用户会话列表列表
     */
    @GetMapping("/list")
    public Result list(UserSessions userSessions,@RequestParam("current") Integer current)
    {
        List<UserSessions> list = userSessionsService.selectUserSessionsList(userSessions, current);
        return Result.ok(list);
    }


    /**
     * 获取用户会话列表详细信息
     */
    @RequiresPermissions("chat:chat:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userSessionsService.selectUserSessionsById(id));
    }

    /**
     * 新增用户会话列表
     */
    @RequiresPermissions("chat:chat:add")
    @Log(title = "用户会话列表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserSessions userSessions)
    {
        return toAjax(userSessionsService.insertUserSessions(userSessions));
    }

    /**
     * 修改用户会话列表
     */
    @RequiresPermissions("chat:chat:edit")
    @Log(title = "用户会话列表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserSessions userSessions)
    {
        return toAjax(userSessionsService.updateUserSessions(userSessions));
    }

    /**
     * 删除用户会话列表
     */
    @Log(title = "用户会话列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public Result remove(@PathVariable("ids") Long[] ids)
    {
        return Result.ok(userSessionsService.deleteUserSessionsByIds(ids));
    }
    //置顶
    @PutMapping("/isPin")
    public Result isPin(@RequestBody UserSessions userSessions){
        boolean pin = userSessionsService.isPin(userSessions);
        if (pin){
            return Result.ok("操作成功");
        }else
        return Result.fail("操作失败");
    }
    /**
     * 修改用户会话背景图
     */
    @PutMapping("/backgroundImage")
    public Result updateBackgroundImage(@RequestBody UserSessions userSessions){
        boolean b = userSessionsService.updateBackgroundImage(userSessions);
        if (!b){
            return Result.fail("操作失败");
        }else
        return Result.ok("操作成功");
    }
}
