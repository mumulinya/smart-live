package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

/**
 * 收藏Star
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
     * @param
     * @param
     * @return
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
      * @param
     * @return
     */
    @GetMapping("/isStar")
    public Result isStar(Star star){
        Boolean isStar = starService.isStar(star);
        return Result.ok(isStar);
    }
    /**
     * 获取收藏列表
     * @param
     * @return
     */
    @GetMapping("/starList")
    public Result getStars(Star star, @RequestParam("current") Integer current){
        return Result.ok(starService.getStarList(star, current));
    }
    /**
     * 获取用户收藏的数量
     * @param
     * @return
     */
    @GetMapping("/getStarCount")
    Result getStarCount(Star star){
        Integer followShopCount=starService.getStarCount(star);
        return Result.ok(followShopCount);
    }
}
