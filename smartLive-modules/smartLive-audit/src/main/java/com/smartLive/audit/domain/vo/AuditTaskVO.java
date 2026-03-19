package com.smartLive.audit.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.audit.domain.AuditTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.Map;

/**
 * 审核任务VO
 */
@Data
public class AuditTaskVO{

    /**
     * 是否高风险
     */
    private Boolean isHighRisk;
    /** 主键ID */
    @TableId
    private Long id;

    /** 业务ID */
    private Long bizId;

    /** 业务类型(1-用户,2-店铺,3-博客,4-代金券,5-评论,6-团购) */
    private Integer bizType;

    /** 提交人ID */
    private Long submitterId;
    /** 提交人名称 */
    private String submitterName;

    /** 状态(0-待审, 1-通过, 2-驳回) */
    private Integer status;

    /** 驳回原因 */
    private String reason;

    /** 审核内容快照(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> auditContent;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 从实体转换为 VO
     *
     * @param entity 审核任务实体
     * @return 审核任务 VO
     */
    public static AuditTaskVO fromEntity(AuditTask entity) {
        if (entity == null) {
            return null;
        }
        AuditTaskVO vo = new AuditTaskVO();
        BeanUtils.copyProperties(entity, vo);
        // 审核快照先拷贝为 Map，后续由策略覆盖
        return vo;
    }
}
