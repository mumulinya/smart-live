package com.smartLive.ai.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * true=通过，false=拒绝，null=未给出明确结果
     */
    private Boolean pass;

    /**
     * 拒绝原因或兜底说明
     */
    private String reason;
}
