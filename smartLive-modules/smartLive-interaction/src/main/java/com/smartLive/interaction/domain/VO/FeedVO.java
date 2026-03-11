package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
/**
 * 关注订阅流视图对象
 * 用于向用户展示其关注对象的动态行为记录及其关联实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedVO{
    /** 数据所属格式类型（BLOG, SHOP 等） */
    private String dataType;
    /** 用户触发的具体动作（发布、点赞等） */
    private String action;
    /** 动作产生的发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    /** 动作关联的源数据实体（由 dataType 决定具体类型） */
    private Object data;
}
