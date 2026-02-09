package com.smartLive.user.domain.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户返回对象 (View Object)
 * 用于前端展示用户基本信息
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
public class UserVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 手机号码 */
    private String phone;

    /** 昵称，默认是用户id */
    private String nickName;

    /** 人物头像 */
    private String icon;

    private Boolean isFollow;

    /** 个性签名 */
    private String introduce;
    
    /** 城市 */
    private String city;
    /** 背景图片 */
    private String backgroundImage;
    
    /** 是否有密码 */
    private Boolean hasPassword;
}