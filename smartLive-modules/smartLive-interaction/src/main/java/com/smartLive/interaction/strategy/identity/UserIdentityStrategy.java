package com.smartLive.interaction.strategy.identity;

import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserIdentityStrategy implements IdentityStrategy<UserDTO> {
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return  IdentityTypeEnum.USER_IDENTITY.getCode();
    }
    /**
     * 获取用户列表
     * @return
     */
    @Override
    public List<SocialInfoVO> getFollowList(List<Long> sourceIdList) {
        List<UserDTO> userList = remoteAppUserService.getUserList(sourceIdList);
        if (userList == null || userList.isEmpty()) {
            return null;
        }
        List<SocialInfoVO> socialInfoVOList = userList.stream().map(user -> SocialInfoVO.builder()
                .id(user.getId())
                .name(user.getNickName())
                .icon(user.getIcon())
                .introduce(user.getIntroduce())
                .isFollow(true)
                .build()
        ).collect(Collectors.toList());
        return socialInfoVOList;
    }
}
