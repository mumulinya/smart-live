package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.domain.LikeRecord;
import com.smartLive.interaction.mapper.FollowMapper;
import com.smartLive.interaction.mapper.LikeRecordMapper;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.interaction.service.ILikeRecordService;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import com.smartLive.user.api.domain.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞记录Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class likeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements ILikeRecordService
{
}
