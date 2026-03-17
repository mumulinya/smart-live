package com.smartLive.ai.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待审核文本内容
     */
    private String content;

    /**
     * 内容类型，当前支持 blog / review
     */
    private String type;
}
