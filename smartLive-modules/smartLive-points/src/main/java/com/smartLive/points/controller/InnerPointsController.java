package com.smartLive.points.controller;

import com.smartLive.common.core.domain.R;
import com.smartLive.points.enums.PointsBizType;
import com.smartLive.points.service.IPointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 积分内部接口Controller（供其他微服务Feign调用）
 *
 * @author smartLive
 */
@RestController
@RequestMapping("/inner/points")
public class InnerPointsController {

    @Autowired
    private IPointsService pointsService;

    /**
     * 新增积分（订单完成时调用）
     *
     * @param userId      用户ID
     * @param amount      积分数量
     * @param bizId       业务ID（订单号）
     * @param description 描述
     * @return 操作结果
     */
    @PostMapping("/add")
    public Boolean addPoints(@RequestParam("userId") Long userId,
                                @RequestParam("amount") Integer amount,
                                @RequestParam("bizId") String bizId,
                                @RequestParam("description") String description) {
        pointsService.addPoints(userId, amount, PointsBizType.CONSUMPTION.getCode(), bizId, description);
        return true;
    }
}
