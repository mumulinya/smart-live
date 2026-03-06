package com.smartLive.user.DTO;

import lombok.Data;

/**
 * 密码修改DTO
 */
@Data
public class PasswordDTO {
    /** 旧密码 */
    private String oldPassword;
    /** 新密码 */
    private String newPassword;
}
