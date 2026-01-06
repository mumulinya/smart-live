package com.smartLive.user.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.service.IUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.user.domain.User;
import com.smartLive.user.service.IUserService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 用户Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController
{
    @Autowired
    private IUserService userService;

    @Autowired
    private IUserInfoService userInfoService;
    /**
     * 分页查询用户列表
     */
    @RequiresPermissions("user:user:list")
    @GetMapping("/list")
    public TableDataInfo list(User user)
    {
        startPage();
        List<User> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @GetMapping("/userList")
    public AjaxResult userList(User user)
    {
        List<User> list = userService.selectUserList(user);
        return success(list);
    }

    /**
     * 导出用户列表
     */
    @RequiresPermissions("user:user:export")
    @Log(title = "用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, User user)
    {
        List<User> list = userService.selectUserList(user);
        ExcelUtil<User> util = new ExcelUtil<User>(User.class);
        util.exportExcel(response, list, "用户数据");
    }

    /**
     * 获取用户详细信息
     */
    @RequiresPermissions("user:user:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userService.selectUserById(id));
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("user:user:add")
    @Log(title = "用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody User user)
    {
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("user:user:edit")
    @Log(title = "用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody User user)
    {
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("user:user:remove")
    @Log(title = "用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(userService.deleteUserByIds(ids));
    }

    /**
     * 全量发布用户信息
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(userService.allPublish());
    }

    /**
     * 发布用户信息
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(userService.publish(ids));
    }
    /**
     * 获取用户详情
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result me(){
        UserDTO userDTO = UserContextHolder.getUser();
        return Result.ok(userDTO);
    }

    /**
     * 根据id查询用户详情
     */
    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Long userId) {
        return Result.ok(userService.queryUserById(userId));
    }

    /**
     * 修改用户信息
     */
    @PostMapping("/update")
    Result updateUser(@RequestBody User user){
        Long userId = UserContextHolder.getUser().getId();
        user.setId(userId);
        return Result.ok(userService.updateUser(user));
    }
    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats/{userId}")
    public Result getStats(@PathVariable("userId") Long userId){
        return Result.ok(userService.getStats(userId));
    }


//    /app/user/info/stats/{userId} - 获取用户统计信息
//
///app/user/follow/fans - 获取粉丝列表
//
///app/user/follow/follows - 获取关注列表
//
///app/user/follow/{userId} - 关注用户
//
///app/user/follow/{userId} - 取消关注
//
///app/review/of/me - 获取我的评价
}
