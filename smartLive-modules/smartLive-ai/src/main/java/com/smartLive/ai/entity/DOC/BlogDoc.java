package com.smartLive.ai.entity.DOC;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 博客文档对象
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class BlogDoc   implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 博客编号 */
    private Long id;

    /** 商户编号 */
    private Long shopId;

    /** 博客类型编号 */
    private Long typeId;

    /** 用户编号 */
    private Long userId;

    /** 标题 */
    private String title;

    /** 探店的照片，最多9张，多张以","隔开 */
    private String images;

    /** 探店的文字描述 */
    private String content;

    /** 点赞数量 */
    private Integer liked;

    /** 评论数量 */
    private Integer comments;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 用户图标
     */
    private String icon;
    /**
     * 用户姓名
     */
    private String name;


    /**
     * 获取格式化的创建时间字符串（用于ES存储）
     */
    public String getCreateTimeFormatted() {
        if (createTime == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(createTime);
    }

    /**
     * 获取时间戳（用于ES存储）
     */
    public Long getCreateTimeTimestamp() {
        return createTime != null ? createTime.getTime() : null;
    }
}
