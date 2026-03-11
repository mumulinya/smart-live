package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.text.Convert;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.BO.ActionRecordBO;
import com.smartLive.interaction.domain.VO.FeedVO;
import com.smartLive.interaction.service.IFeedService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 关注订阅流（Feed）业务实现类
 * 实现了基于 Redis ZSet 的“推拉结合”订阅流模型。
 * 
 * 核心逻辑：
 * 1. 采用滚动分页（Scroll Pagination）避免传统分页在大数据量下的深翻页性能问题。
 * 2. 支持多种动作记录（发布、更新等）的聚合展示。
 * 3. 实现了动态资源策略匹配，透明处理不同业务实体（博客、商品等）的详情装配。
 */
@Service
@Slf4j
public class feedServiceImpl implements IFeedService {
    @Autowired
    RedisService redisService;
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    /**
     * 【核心读链路】分页查询用户的 Feed 流列表
     * 
     * @param feedType 订阅流类型（关注的人、推荐系统等）
     * @param max 上一次查询的最小分值（时间戳），用于滚动游标
     * @param offset 相同分值下的偏移量
     * @return 包含聚合详情与滚动游标的结果集
     */
    @Override
    public ScrollResult queryFeedList(Integer feedType, Long max, Integer offset) {
        if(UserContextHolder.getUser() == null){
            return new ScrollResult();
        }
        //获取当前登录用户
        Long userId = UserContextHolder.getUser().getId();
        FeedTypeEnum feedTypeEnum = FeedTypeEnum.getByCode(feedType);
        String key = feedTypeEnum.getFullKey(userId);

        // 2. 查 Redis (保持原有滚动分页逻辑)
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisService.getCacheZSetReverseRangeByScore(key, 0, max, offset, SystemConstants.MAX_PAGE_SIZE);
        if (tuples == null || tuples.isEmpty()) {
            return new ScrollResult();
        }
        long minTime = 0L;
        int os = 1;
        Map<String, Map<Long, List<ActionRecordBO>>> groupedMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            long time = Objects.requireNonNull(tuple.getScore()).longValue();
            String val = Objects.requireNonNull(tuple.getValue()).toString();

            String[] parts = val.split(":");
            if (parts.length < 2) continue;

            String type = parts[parts.length - 2];
            String action = parts.length > 2 ? parts[0] : "";
            Long id = Long.valueOf(parts[parts.length - 1]);

            // 存入数据
            groupedMap.computeIfAbsent(type, k -> new HashMap<>())
                    .computeIfAbsent(id, k -> new ArrayList<>())
                    .add(new ActionRecordBO(action, time));

            // 滚动分页逻辑
            if (time == minTime) os++;
            else { minTime = time; os = 1; }
        }
        List<FeedVO> voList = Lists.newArrayList();
        groupedMap.forEach((bizType, idActionMap) -> {
            // 1. 获取策略和 Resource 数据（这部分保持不变，依然是批量获取）
            Integer bizTypeEnumCode = GlobalBizTypeEnum.getByBizDomain(bizType).getCode();
            ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(bizTypeEnumCode);

            // 注意：这里传给 getResourceList 的依然是去重后的 ID 集合 (keySet)
            // 比如 ID=101 虽然有 3 个动作，但我们只需要查数据库一次
            List<Object> resources = resourceStrategy.getResourceList(new ArrayList<>(idActionMap.keySet()));

            if (resources != null) {
                resources.forEach(item -> {
                    // 获取对象 ID
                    Long id = resourceStrategy.getResourceId(item);

                    //  获取该 ID 下的 "动作列表"
                    List<ActionRecordBO> actions = idActionMap.get(id);

                    //  遍历动作列表，一个动作生成一个 FeedVO
                    if (actions != null && !actions.isEmpty()) {
                        for (ActionRecordBO node : actions) {

                            //创建返回数据
                            FeedVO feedVO = new FeedVO();

                            // 2.1 保存源数据
                            feedVO.setData(item);

                            // 2.2 设置通用数据
                            feedVO.setDataType(bizType);

                            // 2.3 从 Node 中取出 Action 和 Time
                            feedVO.setAction(node.getAction());
                            feedVO.setPublishTime(new Date(node.getTime())); // 直接使用 Node 里的时间

                            voList.add(feedVO);
                        }
                    }
                });
            }
        });
        voList.sort(Comparator.comparing(FeedVO::getPublishTime).reversed());
        // 6. 返回结果
        ScrollResult result = new ScrollResult();
        result.setList(voList);
        result.setMinTime(minTime);
        result.setOffset(os);
        return result;
    }
}
