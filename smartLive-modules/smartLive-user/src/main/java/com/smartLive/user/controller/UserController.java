package com.smartLive.user.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.user.DTO.PasswordDTO;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.VO.UserInfoVO;
import com.smartLive.user.service.IUserInfoService;
import com.smartLive.user.service.IUserService;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserInfoService userInfoService;

    @RequiresPermissions("user:user:list")
    @GetMapping("/list")
    public TableDataInfo list(User user) {
        startPage();
        List<User> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @GetMapping("/userList")
    public AjaxResult userList(User user) {
        List<User> list = userService.selectUserList(user);
        return success(list);
    }

    @RequiresPermissions("user:user:export")
    @Log(title = "user", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, User user) {
        List<User> list = userService.selectUserList(user);
        ExcelUtil<User> util = new ExcelUtil<>(User.class);
        util.exportExcel(response, list, "user list");
    }

    @RequiresPermissions("user:user:query")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(userService.selectUserById(id));
    }

    @RequiresPermissions("user:user:add")
    @Log(title = "user", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody User user) {
        return toAjax(userService.insertUser(user));
    }

    @RequiresPermissions("user:user:edit")
    @Log(title = "user", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody User user) {
        return toAjax(userService.updateUser(user));
    }

    @RequiresPermissions("user:user:remove")
    @Log(title = "user", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(userService.deleteUserByIds(ids));
    }

    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(userService.allPublish());
    }

    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(userService.publish(ids));
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfoVO infoVO = userInfoService.getByUserId(userId);
        if (infoVO == null) {
            return Result.ok();
        }
        User byId = userService.getById(userId);
        if (byId.getPassword() == null || "".equals(byId.getPassword())) {
            infoVO.setHasPassword(false);
        } else {
            infoVO.setHasPassword(true);
        }
        return Result.ok(infoVO);
    }

    @GetMapping("/me")
    public Result me() {
        AppLoginUser userDTO = UserContextHolder.getUser();
        if (userDTO == null) {
            return Result.fail("user not logged in");
        }
        return Result.ok(userDTO);
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Long userId) {
        return Result.ok(userService.queryUserById(userId));
    }

    @PostMapping("/update")
    Result updateUser(@RequestBody User user) {
        Long userId = UserContextHolder.getUser().getId();
        user.setId(userId);
        return Result.ok(userService.updateUser(user));
    }

    @PostMapping("/updatePassword")
    Result updateUserPassWord(@RequestBody PasswordDTO passwordDTO) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(userService.updateUserPassWord(userId, passwordDTO));
    }

    @PostMapping("/setPassword")
    Result setUserPassWord(@RequestBody User user) {
        Long userId = UserContextHolder.getUser().getId();
        user.setId(userId);
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return Result.ok(userService.updateById(user));
    }

    @GetMapping("/stats/{userId}")
    public Result getStats(@PathVariable("userId") Long userId) {
        return Result.ok(userService.getStats(userId));
    }

    @GetMapping("/search")
    public Result searchUsers(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(userService.searchUsers(keyword));
    }
}