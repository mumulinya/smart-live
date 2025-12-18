package com.smartLive.interaction.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class
AIGenerateRequest {
    private Integer sourceType;
    private List<Long> sourceIds;
}