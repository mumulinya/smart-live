package com.smartlive.chat.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.service.IUserSessionsService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

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
    public Result list(UserSessions userSessions,Integer current)
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
    @RequiresPermissions("chat:chat:remove")
    @Log(title = "用户会话列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(userSessionsService.deleteUserSessionsByIds(ids));
    }
}
