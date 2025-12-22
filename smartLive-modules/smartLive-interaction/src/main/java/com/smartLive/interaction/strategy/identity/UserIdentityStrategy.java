package com.smartLive.interaction.strategy.identity;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.user.User;
import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("userIdentityStrategy")
public class UserIdentityStrategy implements IdentityStrategy<User> {
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public String getType() {
        return  IdentityTypeEnum.USER_IDENTITY.getBizDomain()+"IdentityStrategy";
    }
    /**
     * 获取用户列表
     * @return
     */
    @Override
    public List<SocialInfoVO> getFollowList(List<Long> sourceIdList) {
        R<List<com.smartLive.user.api.domain.User>> userSuccess = remoteAppUserService.getUserList(sourceIdList);
        if (userSuccess.getCode() != 200) {
            return null;
        }
        List<com.smartLive.user.api.domain.User> userList = userSuccess.getData();
        List<SocialInfoVO> socialInfoVOList = userList.stream().map(user -> SocialInfoVO.builder()
                .id(user.getId())
                .name(user.getNickName())
                .avatar(user.getIcon())
                .description(user.getIntroduce())
                .isFollow(true)
                .build()
        ).collect(Collectors.toList());
        return socialInfoVOList;
    }
}
