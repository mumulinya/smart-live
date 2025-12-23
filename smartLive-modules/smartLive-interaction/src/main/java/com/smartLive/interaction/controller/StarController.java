package com.smartLive.interaction.controller;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

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
    private IStarService collectionShopService;
    /**
     * 收藏或取消收藏
     * @param
     * @param
     * @return
     */
    @PutMapping()
    public Result star(@RequestBody Star star) {
        return collectionShopService.star(star);
    }
    /**
     * 查询是否收藏
      * @param
     * @return
     */
    @GetMapping("/isStar")
    public Result isStar(Star star){
        return collectionShopService.isStar(star);
    }
    //获取收藏列表
    @GetMapping("/starList")
    public Result getStars(Star star, @RequestParam("current") Integer current){
        return collectionShopService.getStarList(star, current);
    }
    /**
     * 获取用户收藏的数量
     * @param
     * @return
     */
    @GetMapping("/getStarCount")
    Result getStarCount(Star star){
        Integer followShopCount=collectionShopService.getStarCount(star);
        return Result.ok(followShopCount);
    }
}
