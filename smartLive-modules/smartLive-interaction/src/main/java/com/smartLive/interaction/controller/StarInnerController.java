package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.service.IStarService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏Star
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/star")
public class StarInnerController {
    @Resource
    private IStarService starService;
    /**
     * 查询是否收藏
      * @param
     * @return
     */
    @GetMapping("/isStar")
    public Boolean isStar(Star star){
        Boolean isStar = starService.isStar(star);
        return isStar;
    }
    /**
     * 获取用户收藏的数量
     * @param
     * @return
     */
    @GetMapping("/getStarCount")
    Integer getStarCount(Star star){
        Integer followShopCount=starService.getStarCount(star);
        return followShopCount;
    }
}
