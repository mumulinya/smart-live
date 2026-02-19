package com.smartLive.interaction.controller.api;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.DTO.StarDTO;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

/**
 * 收藏管理外部接口
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/star")
public class StarController {
    @Resource
    private IStarService starService;
    /**
     * 收藏或取消收藏
     * @param star 收藏实体
     * @return 操作结果
     */
    @PutMapping()
    public Result star(@RequestBody Star star) {
        Boolean start = starService.star(star);
        if (start) {
            return Result.ok("操作成功");
        }
        return Result.fail("操作失败");
    }
    /**
     * 查询是否收藏
     * @param star 收藏查询条件
     * @return 是否收藏
     */
    @GetMapping("/isStar")
    public Result isStar(Star star){
        Boolean isStar = starService.isStar(star);
        return Result.ok(isStar);
    }
    /**
     * 获取收藏列表
     * @param star 收藏查询条件
     * @param current 当前页码
     * @return 收藏列表
     */
    @GetMapping("/starList")
    public Result getStars(StarDTO starDTO, @RequestParam("current") Integer current){
        return Result.ok(starService.getStarList(starDTO, current));
    }
    /**
     * 获取用户收藏的数量
     * @param star 收藏查询条件
     * @return 收藏数量
     */
    @GetMapping("/getStarCount")
    Result getStarCount(Star star){
        Integer followShopCount=starService.getStarCount(star);
        return Result.ok(followShopCount);
    }
}
