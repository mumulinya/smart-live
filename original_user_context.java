package com.smartLive.common.core.context;
import com.smartLive.common.core.domain.LoginUser;
/**
 * 用户上下文信息
 *
 * @author smartLive
 */
public final class UserContextHolder {
    private static final ThreadLocal<LoginUser> tl = new ThreadLocal<>();

    // 私有化构造方法，防止被实例化
    private UserContextHolder() {
    }

    public static void saveUser(LoginUser user){
        tl.set(user);
    }

    public static LoginUser getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
