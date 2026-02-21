package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeVO {
    /**
     * 点赞时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date likeTime;  // 点赞时间
    /**
     * 源数据
     */
    private Object data;

}