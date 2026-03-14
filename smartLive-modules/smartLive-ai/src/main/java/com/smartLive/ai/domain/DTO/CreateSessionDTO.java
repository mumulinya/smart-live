package com.smartLive.ai.domain.DTO;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateSessionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shopId;

    private String type;

    private String title;

    private Long userId;
}