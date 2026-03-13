package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * AI 鑱婂ぉ娑堟伅瀹炰綋
 * 瀵瑰簲鏁版嵁搴?message 琛紝瀛樺偍浼氳瘽涓殑姣忎竴杞璇濆唴瀹?
 *
 * @author smartLive
 */
@Data
@ToString(callSuper = true)
@TableName("message")
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 娑堟伅 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 鎵€灞炰細璇?ID */
    private Long sessionId;

    /** 瑙掕壊 (user:鐢ㄦ埛, assistant:AI鍔╂墜, system:绯荤粺璇存槑) */
    private String role;

    /** 娑堟伅姝ｆ枃鍐呭 */
    private String content;

    /** 娑堟伅绫诲瀷 (text:绾枃鏈? card:鎺ㄨ崘鍗＄墖, image:鍥剧墖) */
    private String type;

    /** 鍒涘缓鏃堕棿 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
