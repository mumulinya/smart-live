package com.smartLive.interaction.domain.BO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionRecordBO {
        String action;
        long time;
}