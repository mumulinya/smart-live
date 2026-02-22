package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * LikeVO类，用于封装点赞相关的数据信息
 * 使用了Lombok的@Data注解自动生成getter、setter等方法
 * 使用了@AllArgsConstructor和@NoArgsConstructor注解自动生成全参构造函数和无参构造函数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 *  LikeVO类，表示一个点赞的数据对象
 * 包含点赞时间和源数据两个主要属性
 */
public class LikeVO {
    /**
     * 点赞时间
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date likeTime;  // 点赞时间
    /**
     * 源数据
     * 可以存储任意类型的点赞相关数据
     */
    private Object data;

}