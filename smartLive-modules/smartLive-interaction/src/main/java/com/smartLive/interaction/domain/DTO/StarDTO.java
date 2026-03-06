package com.smartLive.interaction.domain.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 收藏 DTO
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StarDTO extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 来源类型  1（文章）, 3（评论）, 4（代金券）等。 */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    /** 来源名称 */
    private String sourceName;

    /** 来源id */
    @Excel(name = "来源id")
    private Long sourceId;

    /** 是否收藏 */
    private Boolean isStar;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
