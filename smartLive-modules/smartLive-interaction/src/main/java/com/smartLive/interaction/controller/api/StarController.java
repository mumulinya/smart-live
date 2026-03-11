package com.smartLive.interaction.controller.api;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.DTO.StarDTO;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;


/**
 * 收藏管理控制层
 * 提供对店铺、商品、博客等资源的收藏（Star/Bookmark）功能，支持状态查询、批量校验及列表分页展示。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/star")
public class StarController {
    @Resource
    private IStarService starService;
    /**
     * 收藏或取消收藏
     * @param starDTO 收藏DTO（包含isStar标识）
     * @return 操作结果
     */
    @PutMapping()
    public Result star(@RequestBody StarDTO starDTO) {
        Boolean start = starService.star(starDTO);
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
     * 查询收藏用户列表
     * @param star 收藏查询条件
     * @return 收藏用户列表
     */
    @GetMapping("/starUserList")
    public Result queryStarUserList(Star star){
        return Result.ok(starService.queryStarUserList(star));
    }
    /**
     * 获取用户收藏的数量
     * @param star 收藏查询条件
     * @return 收藏数量
     */
    @GetMapping("/count")
    public Result getStarCount(Star star) {
        Integer followShopCount = starService.getStarCount(star);
        return Result.ok(followShopCount);
    }

    /**
     * 批量查询资源是否已收藏
     * 优化性能，减少前端在列表页中发起的并发请求次数。
     *
     * @param starDTO   业务类型基础信息
     * @param sourceIds 资源 ID 列表
     * @return ID 与收藏状态的映射结果
     */
    @GetMapping("/getIsStarBatch")
    public Result getIsStarBatch(StarDTO starDTO, @RequestParam("sourceIds") List<Long> sourceIds){
        return Result.ok(starService.isStarBatch(starDTO, sourceIds));
    }
}
