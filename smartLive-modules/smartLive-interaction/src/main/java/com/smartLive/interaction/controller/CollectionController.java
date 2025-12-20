package com.smartLive.interaction.controller;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Collection;
import com.smartLive.interaction.service.ICollectionService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/**
 * 收藏Controller
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/collection")
public class CollectionController {
    @Resource
    private ICollectionService collectionShopService;
    /**
     * 收藏或取消收藏
     * @param
     * @param
     * @return
     */
    @PutMapping()
    public Result collection(Collection collection) {
        return collectionShopService.collection(collection);
    }
    /**
     * 查询是否收藏
      * @param
     * @return
     */
    @GetMapping("/isCollection")
    public Result isCollection(Collection collection){
        return collectionShopService.isCollection(collection);
    }
    //获取收藏列表
    @GetMapping("/collectionList")
    public Result getFollows(Collection collection,@RequestParam("current") Integer current){
        return collectionShopService.getCollectionList(collection, current);
    }
    /**
     * 获取用户收藏的数量
     * @param
     * @return
     */
    @GetMapping("/getCollentCount")
    Result getFollowShopCount(Collection collection){
        Integer followShopCount=collectionShopService.getCollectCount(collection);
        return Result.ok(followShopCount);
    }
}
