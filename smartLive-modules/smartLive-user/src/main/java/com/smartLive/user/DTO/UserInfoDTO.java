package com.smartLive.user.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 用户信息DTO
 */
public class UserInfoDTO {
    
    private Long userId;
    private String city;
    private String introduce;
    private Integer fans;
    private Integer followee;
    private Integer gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    
    private String credits;
    private String level;
    
    // 构造方法
    public UserInfoDTO() {}
    
    // getter和setter方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    public Integer getFans() {
        return fans;
    }

    public void setFans(Integer fans) {
        this.fans = fans;
    }

    public Integer getFollowee() {
        return followee;
    }

    public void setFollowee(Integer followee) {
        this.followee = followee;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "UserInfoDTO{" +
                "userId=" + userId +
                ", city='" + city + '\'' +
                ", introduce='" + introduce + '\'' +
                ", fans=" + fans +
                ", followee=" + followee +
                ", gender=" + gender +
                ", birthday=" + birthday +
                ", credits='" + credits + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
}