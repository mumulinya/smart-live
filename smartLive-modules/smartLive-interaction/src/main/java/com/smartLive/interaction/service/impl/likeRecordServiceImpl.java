package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.domain.LikeRecord;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.interaction.mapper.FollowMapper;
import com.smartLive.interaction.mapper.LikeRecordMapper;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.interaction.service.ILikeRecordService;
import com.smartLive.interaction.strategy.follow.InfoFetcherStrategy;
import com.smartLive.interaction.strategy.resource.ResourceFetcherStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞记录Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class likeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements ILikeRecordService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Map<String, InfoFetcherStrategy> infoFetcherStrategyMap;
    @Autowired
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
    @Autowired
    private Map<String, ResourceFetcherStrategy> resourceStrategyMap;

    /**
     * 点赞或取消点赞
     *
     * @param likeRecord
     * @return 点赞记录
     */
    @Override
    public Boolean likeOrCancelLike(LikeRecord likeRecord) {
        //获取当前登录用户
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录
            return false;
        }
        Long userId = user.getId();
        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(likeRecord.getSourceType());
        String likeKeyPrefix = resourceTypeEnum.getLikeKeyPrefix();
        String likedCountKeyPrefix = resourceTypeEnum.getLikedCountKeyPrefix();
        String likeDirtyKeyPrefix = resourceTypeEnum.getLikeDirtyKeyPrefix();

        //判断当前用户是否已经点赞
        String key = likeKeyPrefix + likeRecord.getSourceType()+":"+likeRecord.getSourceId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score!=null) {
            //删除点赞记录
            boolean isDelete = removeById(likeRecord);
            if (isDelete) {
                //删除用户点赞信息
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
                //记录点赞数量
                stringRedisTemplate.opsForValue().decrement(likedCountKeyPrefix+likeRecord.getSourceId());
                //记录脏数据
                stringRedisTemplate.opsForSet().add(likeDirtyKeyPrefix, likeRecord.getSourceId().toString());
            }
        }else{
            //未点赞
            boolean isSuccess = save(likeRecord);
            //保存用户点赞信息到redis的set集合 zadd key value score
            if (isSuccess) {
                //保存用户点赞信息
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
                //记录点赞数量
                stringRedisTemplate.opsForValue().increment(likedCountKeyPrefix+likeRecord.getSourceId());
                //记录脏数据
                stringRedisTemplate.opsForSet().add(likeDirtyKeyPrefix, likeRecord.getSourceId().toString());
            }
        }
        //清空缓存
//        flashRedisBlogCache(id);
//        flashRedisBlogListCache();
//        return Result.ok("点赞成功");
        return true;
    }

    /**
     * 查询点赞数
     *
     * @param likeRecord@return 点赞数
     */
    @Override
    public Integer queryLikeCount(LikeRecord likeRecord) {
        Integer likeCount = query().eq("source_type", likeRecord.getSourceType()).eq("source_id", likeRecord.getSourceId()).count().intValue();
        return likeCount;
    }

    /**
     * 查询点赞列表
     *
     * @param likeRecord@return 点赞记录
     */
    @Override
    public List<ResourceVO> queryLikeRecord(LikeRecord likeRecord,Integer current) {
        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(likeRecord.getSourceType());
        ResourceFetcherStrategy resourceFetcherStrategy = resourceStrategyMap.get(resourceTypeEnum.getStrategyName());
        List<Long> sourceIdList = query().select("source_id").eq("source_type", likeRecord.getSourceType()).eq("user_id",likeRecord.getUserId()).list().stream().map(LikeRecord::getSourceId).collect(Collectors.toList());
        List<ResourceVO> resourceVOList = resourceFetcherStrategy.getResourceList(sourceIdList);
        return resourceVOList;
    }

    /**
     * 查询点赞用户列表
     *
     * @param likeRecord@return 点赞用户列表
     */
    @Override
    public List<SocialInfoVO> queryLikeUserList(LikeRecord likeRecord) {
        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(likeRecord.getSourceType());
        String likeKeyPrefix = resourceTypeEnum.getLikeKeyPrefix();
        String key = likeKeyPrefix + likeRecord.getSourceType() + ":" + likeRecord.getSourceId();
        //查询top5的点赞数 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return null;
        }
        InfoFetcherStrategy infoFetcherStrategy = infoFetcherStrategyMap.get(FollowTypeEnum.USERINFO.getStrategyName());
        //解析其中的用户id
        List<Long> userIdList = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        List<SocialInfoVO> socialInfoVOList = infoFetcherStrategy.getFollowList(userIdList);
        return socialInfoVOList;
    }
}
