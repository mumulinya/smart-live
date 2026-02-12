package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserResourceStrategy implements ResourceStrategy<UserDTO> {
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return  FollowTypeEnum.USER_IDENTITY.getCode();
    }
    /**
     * 获取用户列表
     * @return
     */
    @Override
    public List<UserDTO> getResourceList(List<Long> sourceIdList) {
        List<UserDTO> userList = remoteAppUserService.getUserList(sourceIdList);
        if (userList == null || userList.isEmpty()) {
            return null;
        }
        return userList;
    }

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public UserDTO getResourceById(Long sourceId) {
        UserDTO user = remoteAppUserService.queryUserById(sourceId);
        if (user == null) {
            return null;
        }
        return user;
    }
}
