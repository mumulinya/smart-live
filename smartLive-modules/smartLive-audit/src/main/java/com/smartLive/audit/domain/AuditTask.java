package com.smartLive.audit.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 审核任务实体对象 tb_audit_task
 */
@Data
@Accessors(chain = true)
@TableName(value = "audit_task", autoResultMap = true)
public class AuditTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId
    private Long id;

    /** 业务ID */
    private Long bizId;

    /** 业务类型(1-用户,2-店铺,3-博客,4-代金券,5-评论,6-团购) */
    private Integer bizType;

    /** 提交人ID */
    private Long submitterId;

    /** 状态(0-待审核, 1-审核通过, 2-人工驳回, 3-自动驳回) */
    private Integer status;

    /** 驳回原因 */
    private String reason;

    /** 审核内容快照(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> auditContent;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
