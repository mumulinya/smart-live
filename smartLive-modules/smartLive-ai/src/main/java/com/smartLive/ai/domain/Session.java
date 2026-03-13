package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 鑱婂ぉ浼氳瘽瀹炰綋
 * 瀵瑰簲鏁版嵁搴?session 琛紝鐢ㄤ簬绠＄悊鐢ㄦ埛鐨勫璇濊褰曞垪琛?
 *
 * @author smartLive
 */
@Data
@ToString(callSuper = true)
@TableName("session")
public class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 浼氳瘽 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 鎵€灞炵敤鎴?ID */
    private Long userId;

    /** 浼氳瘽鏍囬锛堝锛氱編椋熸帹鑽愬挩璇級 */
    private String title;

    /** 鍒涘缓鏃堕棿 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 鏈€鍚庢洿鏂版椂闂达紙鐢ㄤ簬浼氳瘽鍒楄〃鎺掑簭锛?*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
