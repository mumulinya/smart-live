package com.smartLive.user.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import com.smartLive.user.api.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RemoteAppUserFallbackFactory implements FallbackFactory<RemoteAppUserService> {
    @Override
    public RemoteAppUserService create(Throwable throwable) {
        return new RemoteAppUserService() {
            /**
             * 根据用户电话号码查询用户
             *
             * @param phone
             * @return
             */
            @Override
            public UserDTO getUserInfoByPhone(String phone) {
                log.error("查询用户信息失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 电话号码创建用户
             *
             * @param phone
             * @return
             */
            @Override
            public UserDTO createUserByPhone(String phone) {
                log.error("创建用户失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 根据用户id字符串查询用户列表
             *
             * @param userIdList
             * @return
             */
            @Override
            public List<UserDTO> getUserList(List<Long> userIdList) {
                log.error("查询用户列表失败:{}", throwable.getMessage());
                return null;
            }

            /**
             * 根据用户id查询用户
             *
             * @param id
             * @return
             */
            @Override
            public UserDTO queryUserById(Long id) {
                log.error("查询用户失败:{}", throwable.getMessage());
                return null;
            }
            /**
             * 根据用户id查询用户名称
             *
             * @param userId
             * @return
             */
            @Override
            public String getUserNameById(Long userId) {
                log.error("查询用户名称失败:{}", throwable.getMessage());
                return "";
            }
        };
    }
}
