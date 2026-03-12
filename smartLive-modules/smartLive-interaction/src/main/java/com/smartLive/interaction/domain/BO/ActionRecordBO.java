package com.smartLive.interaction.domain.BO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行为记录业务对象
 * 封装了用户或系统执行的特定行为及其发生时间。
 * 用于记录和追踪系统中的关键操作。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionRecordBO {
    /** 店铺ID 店铺上新 */
    Long shopId;
    /** 行为类型或名称，例如 "上新", "下架", "即将下架" */
    String action;
    /** 行为发生的时间戳，通常为毫秒级 */
    long time;
}