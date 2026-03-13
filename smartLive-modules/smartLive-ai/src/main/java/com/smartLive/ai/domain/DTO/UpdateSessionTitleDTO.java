package com.smartLive.ai.domain.DTO;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateSessionTitleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
}
