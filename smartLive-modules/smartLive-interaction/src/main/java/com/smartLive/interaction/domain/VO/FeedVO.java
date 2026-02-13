package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedVO{
    /**
     * 数据类型
     */
    private String dataType;
    /**
     * 动作
     */
    private String action;
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;  // 发布时间
    /**
     * 源数据
     */
    private Object data;
}
