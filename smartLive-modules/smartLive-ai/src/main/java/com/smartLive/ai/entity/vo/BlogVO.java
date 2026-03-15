package com.smartLive.ai.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BlogVO {

    private Long id;

    private Long shopId;

    private Long typeId;

    private Long userId;

    private String title;

    private String images;

    private String content;

    private Integer liked;

    private Integer comments;

    private String icon;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private Float relevanceScore;
}