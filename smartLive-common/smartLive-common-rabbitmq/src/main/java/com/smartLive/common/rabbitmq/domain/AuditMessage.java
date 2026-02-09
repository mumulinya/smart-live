package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 审核消息实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 业务类型(1-用户,2-店铺,3-博客,4-代金券,5-评论,6-团购)
     */
    private Integer bizType;

    /**
     * 提交人ID
     */
    private Long submitterId;

    /**
     * 审核内容快照
     */
    private Map<String, Object> auditContent;

    /**
     * 提交时间
     */
    private Date createTime;
}
