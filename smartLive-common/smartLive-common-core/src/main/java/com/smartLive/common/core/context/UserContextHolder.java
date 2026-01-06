package com.smartLive.common.core.context;
import com.smartLive.common.core.domain.UserDTO;
/**
 * 用户上下文信息
 *
 * @author smartLive
 */
public class UserContextHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
