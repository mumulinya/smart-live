package com.smartLive.user.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemoteAppUserFallbackFactory implements FallbackFactory<RemoteAppUserService> {
    @Override
    public RemoteAppUserService create(Throwable throwable) {
        return new RemoteAppUserService() {
            @Override
            public R<User> getUserInfoByPhone(String phone) {
                return R.fail("获取用户信息失败");
            }

            @Override
            public R<User> createUserByPhone(String phone) {
                return R.fail("创建用户失败");
            }
            @Override
            public void sendBlogToFollowers(BlogDTO blog) {
                throw new RuntimeException("发送博客失败");
            }

            @Override
            public R<List<User>> getUserList(List<Long> userIdList) {
                return R.fail("获取用户列表失败");
            }
            @Override
            public R<User> queryUserById(Long id) {
                return R.fail("查询用户失败");
            }
        };
    }
}
