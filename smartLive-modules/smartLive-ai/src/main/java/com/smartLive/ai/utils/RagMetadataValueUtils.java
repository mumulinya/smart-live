package com.smartLive.ai.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * RAG 元数据值工具类。
 */
@Slf4j
public final class RagMetadataValueUtils {

    /**
     * 构造 RAG 元数据值工具类。
     */
    private RagMetadataValueUtils() {
    }

    /**
     * 对象转字符串，null 返回 null。
     */
    public static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 对象转 Long，无法转换时返回 null。
     */
    public static Long toLong(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.longValue();
    }

    /**
     * 对象转 Integer，无法转换时返回 null。
     */
    public static Integer toInteger(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.intValue();
    }

    /**
     * 对象转 Double，无法转换时返回 null。
     */
    public static Double toDouble(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.doubleValue();
    }

    /**
     * 对象转 BigDecimal，空值或非法数值返回 null。
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse numeric metadata value: {}", text);
            return null;
        }
    }

    /**
     * 字符串转 Date 工具方法。
     */
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }
}
