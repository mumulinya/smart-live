package com.smartLive.user.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息返回对象 (View Object)
 * 用于前端展示用户详细信息
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
public class UserInfoVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键，用户id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 城市名称 */
    private String city;

    /** 个人介绍，不要超过128个字符 */
    private String introduce;

    /** 粉丝数量 */
    private Integer fans;

    /** 关注的人的数量 */
    private Integer followee;

    /** 性别，0：男，1：女 */
    private Integer gender;

    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    /** 积分 */
    private String credits;

    /** 会员级别，0~9级,0代表未开通会员 */
    private String level;
    
    /** 背景图片 */
    private String backgroundImage;
    
    /** 是否有密码 */
    private Boolean hasPassword;
}