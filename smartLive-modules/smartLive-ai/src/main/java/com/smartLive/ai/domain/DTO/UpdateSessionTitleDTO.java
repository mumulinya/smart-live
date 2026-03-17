package com.smartLive.ai.domain.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新会话标题数据传输对象。
 */
@Data
public class UpdateSessionTitleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
}
