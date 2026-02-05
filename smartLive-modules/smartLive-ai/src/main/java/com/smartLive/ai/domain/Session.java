package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * AI Chat Session Entity
 *
 * @author smartLive
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("ai_session")
public class Session extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** Session ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** User ID */
    private Long userId;

    /** Session Title */
    private String title;
}
