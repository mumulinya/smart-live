package com.smartLive.points.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.points.domain.vo.LotteryConfigVO;
import com.smartLive.points.domain.vo.LotteryResultVO;
import com.smartLive.points.domain.vo.WalletInfoVO;
import com.smartLive.points.service.IPointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 积分Controller
 *
 * @author smartLive
 */
@RestController
@RequestMapping("/points")
public class PointsController {

    @Autowired
    private IPointsService pointsService;

    /**
     * 获取积分中心信息
     */
    @GetMapping("/wallet/info")
    public Result getWalletInfo() {
        Long userId = UserContextHolder.getUser().getId();
        WalletInfoVO info = pointsService.getWalletInfo(userId);
        return Result.ok(info);
    }

    /**
     * 获取用户积分明细列表
     */
    @GetMapping("/record/listByUserId")
    public Result getRecordList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "type", defaultValue = "all") String type) {
        Long userId = UserContextHolder.getUser().getId();
        Map<String, Object> data = pointsService.getRecordList(userId, page, pageSize, type);
        return Result.ok(data);
    }

    /**
     * 每日签到
     */
    @PostMapping("/sign_in")
    public Result signIn() {
        Long userId = UserContextHolder.getUser().getId();
        Map<String, Object> data = pointsService.signIn(userId);
        return Result.ok(data);
    }

    /**
     * 获取抽奖配置
     */
    @GetMapping("/lottery/config")
    public Result getLotteryConfig() {
        LotteryConfigVO config = pointsService.getLotteryConfig();
        return Result.ok(config);
    }

    /**
     * 积分抽奖
     */
    @PostMapping("/lottery/draw")
    public Result lotteryDraw() {
        Long userId = UserContextHolder.getUser().getId();
        LotteryResultVO result = pointsService.lotteryDraw(userId);
        return Result.ok(result);
    }
}
