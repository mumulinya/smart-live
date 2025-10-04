package com.smartLive.user.domain;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

/**
 * 用户对象 tb_user_info
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@TableName("tb_user_info")
public class UserInfo extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键，用户id */
    private Long userId;

    /** 城市名称 */
    @Excel(name = "城市名称")
    private String city;

    /** 个人介绍，不要超过128个字符 */
    @Excel(name = "个人介绍，不要超过128个字符")
    private String introduce;

    /** 粉丝数量 */
    @Excel(name = "粉丝数量")
    private Integer fans;

    /** 关注的人的数量 */
    @Excel(name = "关注的人的数量")
    private Integer followee;

    /** 性别，0：男，1：女 */
    @Excel(name = "性别，0：男，1：女")
    private Integer gender;

    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生日", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthday;

    /** 积分 */
    @Excel(name = "积分")
    private String credits;

    /** 会员级别，0~9级,0代表未开通会员 */
    @Excel(name = "会员级别，0~9级,0代表未开通会员")
    private String level;

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setIntroduce(String introduce) 
    {
        this.introduce = introduce;
    }

    public String getIntroduce() 
    {
        return introduce;
    }

    public void setFans(Integer fans) 
    {
        this.fans = fans;
    }

    public Integer getFans() 
    {
        return fans;
    }

    public void setFollowee(Integer followee) 
    {
        this.followee = followee;
    }

    public Integer getFollowee() 
    {
        return followee;
    }

    public void setGender(Integer gender) 
    {
        this.gender = gender;
    }

    public Integer getGender() 
    {
        return gender;
    }

    public void setBirthday(Date birthday) 
    {
        this.birthday = birthday;
    }

    public Date getBirthday() 
    {
        return birthday;
    }

    public void setCredits(String credits) 
    {
        this.credits = credits;
    }

    public String getCredits() 
    {
        return credits;
    }

    public void setLevel(String level) 
    {
        this.level = level;
    }

    public String getLevel() 
    {
        return level;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("city", getCity())
            .append("introduce", getIntroduce())
            .append("fans", getFans())
            .append("followee", getFollowee())
            .append("gender", getGender())
            .append("birthday", getBirthday())
            .append("credits", getCredits())
            .append("level", getLevel())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
