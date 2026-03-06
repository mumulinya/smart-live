package com.smartLive.interaction.domain.DTO;

import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注 DTO
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowDTO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 来源类型 */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    /** 来源名称 */
    private String sourceName;

    /** 来源id */
    @Excel(name = "来源id")
    private Long sourceId;

    /** 是否关注 */
    private Boolean isFollow;

    /** 创建时间 */
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public FollowDTO(Integer sourceType, Long sourceId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }
}
