package com.smartLive.shop.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.mapper.ShopTypeMapper;
import com.smartLive.shop.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 木木林
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private RedisService redisService;

    /**
     * 查询所有商铺类型
     *
     * @return 商铺类型列表
     */
    @Override
    public Result queryList() {
        //从缓存中获取
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        //从缓存中获取商户类型列表
        String shopTypeListJson =  redisService.leftPopCacheList(key);
        if(StrUtil.isNotBlank(shopTypeListJson)){
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeListJson, ShopType.class);
            return Result.ok(shopTypeList);
        }
        List<ShopType> typeList = this
                .query().orderByAsc("sort").list();
        if(typeList == null){
            return Result.fail("商铺类型不存在");
        }
        redisService.setCacheList(key, JSONUtil.toJsonStr(typeList));
        redisService.expire(key, RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(typeList);
    }
}
