package com.smartLive.interaction.controller.inner;

import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import jakarta.annotation.Resource;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import com.smartLive.interaction.domain.DTO.StarDTO;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务内部控制层
 * 为微服务架构提供收藏状态查询、统计及批量校验等 RPC 接口，主要由 Shop、Product 等模块通过 Feign 调用。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/inner/star")
public class StarInnerController {
    @Resource
    private IStarService starService;
    /**
     * 查询是否收藏
     * @param star 收藏查询条件
     * @return 是否收藏
     */
    @GetMapping("/isStar")
    public Boolean isStar(Star star){
        Boolean isStar = starService.isStar(star);
        return isStar;
    }
    /**
     * 获取收藏的数量
     * @param star 收藏查询条件
     * @return 收藏数量
     */
    @GetMapping("/getStarCount")
    Integer getStarCount(Star star){
        return starService.getStarCount(star);
    }
    /**
     * 获取用户收藏数量
     * @param star 收藏查询条件
     * @return 用户收藏数量
     */
    @GetMapping("/getUserStarCount")
    Integer getUserStarCount(@SpringQueryMap Star star){
        return starService.getUserStarCount(star);
    }

    /**
     * 查询收藏用户列表
     * @param star 收藏查询条件
     * @return 收藏用户列表
     */
    @GetMapping("/starUserList")
    public List<?> queryStarUserList(Star star){
        return starService.queryStarUserList(star);
    }

    /**
     * 批量查询是否收藏
     * @param starDTO
     * @param sourceIds
     * @return
     */
    @GetMapping("/getIsStarBatch")
    public Map<Long, Boolean> getIsStarBatch(StarDTO starDTO, @RequestParam("sourceIds") List<Long> sourceIds) {
        return starService.isStarBatch(starDTO, sourceIds);
    }
}
