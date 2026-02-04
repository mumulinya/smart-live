package com.smartLive.job.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务调度日志返回对象 (View Object)
 * 用于前端展示定时任务日志详情
 * 
 * @author smartLive
 */
@Data
public class SysJobLogVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobLogId;

    /** 任务名称 */
    private String jobName;

    /** 任务组名 */
    private String jobGroup;

    /** 调用目标字符串 */
    private String invokeTarget;

    /** 日志信息 */
    private String jobMessage;

    /** 执行状态（0正常 1失败） */
    private String status;

    /** 异常信息 */
    private String exceptionInfo;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 停止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date stopTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}