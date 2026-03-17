package com.smartLive.shop.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 差评信息视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadReviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;

    private Integer score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}