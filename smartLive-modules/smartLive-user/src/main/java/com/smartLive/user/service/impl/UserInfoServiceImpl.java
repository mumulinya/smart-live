package com.smartLive.user.service.impl;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.user.DTO.UserInfoDTO;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.domain.VO.UserInfoVO;
import com.smartLive.user.domain.VO.UserVO;
import com.smartLive.user.mapper.UserInfoMapper;
import com.smartLive.user.service.IUserInfoService;
import com.smartLive.user.service.IUserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户信息Service实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    private IUserService userService;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    //使用懒加载，避免循环引用
    public UserInfoServiceImpl(@Lazy IUserService userService) {
        this.userService = userService;
    }

    /**
     * 将UserInfo实体转换为UserInfoVO
     * @param userInfo UserInfo实体
     * @return UserInfoVO对象
     */
    private UserInfoVO convertToUserInfoVO(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(userInfo, userInfoVO);
        return userInfoVO;
    }

    /**
     * 将UserInfo列表转换为UserInfoVO列表
     * @param userInfoList UserInfo实体列表
     * @return UserInfoVO列表
     */
    private List<UserInfoVO> convertToUserInfoVOList(List<UserInfo> userInfoList) {
        if (userInfoList == null || userInfoList.isEmpty()) {
            return new ArrayList<>();
        }
        return userInfoList.stream()
                .map(this::convertToUserInfoVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserInfoVO getByUserId(Long userId) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserInfo userInfo = getOne(queryWrapper);
        return convertToUserInfoVO(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserInfo userInfo) {
        Long userId = UserContextHolder.getUser().getId();
        userInfo.setUserId(userId);
        if (userInfo.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        UserInfoVO user = getByUserId(userId);
        //没有数据，创建数据
        if(user==null){
            boolean saved = save(userInfo);
            if (saved) {
                userService.clearUserCache(userId);
            }
            return saved;
        }
        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        if (userInfo.getCity() != null){
            updateWrapper.set("city", userInfo.getCity());
        }
        if (userInfo.getIntroduce() != null){
            updateWrapper.set("introduce", userInfo.getIntroduce());
        }
        if (userInfo.getGender() != null){
            updateWrapper.set("gender", userInfo.getGender());
        }
        if (userInfo.getBirthday() != null){
            updateWrapper.set("birthday", userInfo.getBirthday());
        }
        if (userInfo.getCredits() != null){
            updateWrapper.set("credits", userInfo.getCredits());
        }
        if (userInfo.getLevel() != null){
            updateWrapper.set("level", userInfo.getLevel());
        }
        updateWrapper.set("update_time", new Date());
        boolean update = update(updateWrapper);
        if (update){
            userService.clearUserCache(userId);
            //更新用户信息成功，更新es数据
           userService.publish(new String[]{userId.toString()});
            User userById = userService.selectUserById(userId);
            UserInfoVO userInfoVO = getByUserId(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(userById, userVO);
            userVO.setIntroduce(userInfoVO.getIntroduce());
            userVO.setCity(userInfoVO.getCity());
            userVO.setBackgroundImage(userInfoVO.getBackgroundImage());
            AuditMessage auditMessage = AuditMessage.builder()
                    .bizId(userById.getId())
                    .bizType(GlobalBizTypeEnum.USER.getCode())
                    .submitterId(userById.getId())
                    .auditContent(BeanUtil.beanToMap(userVO))
                    .createTime(userById.getCreateTime())
                    .build();
            mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_EXCHANGE_NAME,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
        }
        return update;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCity(Long userId, String city) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setCity(city);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateIntroduce(Long userId, String introduce) {
        // 验证个人介绍长度
        if (introduce != null && introduce.length() > 128) {
            throw new RuntimeException("个人介绍不能超过128个字符");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setIntroduce(introduce);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGender(Long userId, Integer gender) {
        // 验证性别参数
        if (gender != null && gender != 0 && gender != 1) {
            throw new RuntimeException("性别参数错误：0-男，1-女");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setGender(gender);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBirthday(Long userId, Date birthday) {
        // 验证生日不能超过今天
        if (birthday != null && birthday.after(new Date())) {
            throw new RuntimeException("生日不能超过今天");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setBirthday(birthday);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCredits(Long userId, String credits) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setCredits(credits);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLevel(Long userId, String level) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setLevel(level);
        userInfo.setUpdateTime(new Date());
        boolean updated = updateById(userInfo);
        if (updated) {
            userService.clearUserCache(userId);
        }
        return updated;
    }

    /**
     * 根据用户ID列表查询用户信息列表
     *
     * @param userIds
     */
    @Override
    public List<UserInfoVO> listByUserIds(List<Long> userIds) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = list(queryWrapper);
        return convertToUserInfoVOList(userInfoList);
    }

    /**
     * 更新用户背景图片
     *
     * @param userInfoDTO
     */
    @Override
    public Boolean updateBackgroundImage(UserInfoDTO userInfoDTO) {
        boolean update = update().set("background_image", userInfoDTO.getBackgroundImage())
                .eq("user_id", userInfoDTO.getUserId())
                .update();
        if (update && userInfoDTO.getUserId() != null) {
            userService.clearUserCache(userInfoDTO.getUserId());
        }
        return update;
    }
    /**
     * 更新用户状态
     * @param id 用户ID
     * @param status 状态
     * @return
     */
    @Override
    public Boolean updateUserStatus(Long id, Integer status) {
        boolean updated = update(new UpdateWrapper<UserInfo>()
                .set("status", status)
                .eq("user_id", id));
        if (updated) {
            userService.clearUserCache(id);
        }
        return updated;
    }

    @Override
    public Boolean updateFansCountBatch(Map<Long, Integer> updateMap) {
        if (updateMap == null || updateMap.isEmpty()) {
            return false;
        }
        batchUpdate(updateMap, true);
        updateMap.keySet().forEach(userService::clearUserCache);
        return true;
    }

    @Override
    public Boolean updateFolloweeCountBatch(Map<Long, Integer> updateMap) {
        if (updateMap == null || updateMap.isEmpty()) {
            return false;
        }
        batchUpdate(updateMap, false);
        updateMap.keySet().forEach(userService::clearUserCache);
        return true;
    }

    private void batchUpdate(Map<Long, Integer> updateMap, boolean fans) {
        List<Long> keys = new ArrayList<>(updateMap.keySet());
        int batchSize = 500;
        for (int i = 0; i < keys.size(); i += batchSize) {
            int end = Math.min(i + batchSize, keys.size());
            Map<Long, Integer> batchMap = keys.subList(i, end).stream()
                    .collect(Collectors.toMap(k -> k, updateMap::get));
            if (fans) {
                baseMapper.updateFansCountBatch(batchMap);
            } else {
                baseMapper.updateFolloweeCountBatch(batchMap);
            }
        }
    }
}
