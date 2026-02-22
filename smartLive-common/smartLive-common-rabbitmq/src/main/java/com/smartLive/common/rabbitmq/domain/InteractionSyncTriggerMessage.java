package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionSyncTriggerMessage implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Trigger source, e.g. quartz job or manual dispatch.
     */
    private String source;

    /**
     * Epoch milliseconds when the trigger message is produced.
     */
    private Long triggerTime;
}
