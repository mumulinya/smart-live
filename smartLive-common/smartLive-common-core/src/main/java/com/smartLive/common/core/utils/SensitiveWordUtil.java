package com.smartLive.common.core.utils;

import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 敏感词过滤工具类
 * 封装了 com.github.houbb:sensitive-word 的核心功能
 */
@Component
public class SensitiveWordUtil {

    /**
     * 判断是否包含敏感词
     * @param text 待检测文本
     * @return true=包含(不安全), false=不包含(安全)
     */
    public boolean hasSensitiveWord(String text) {
        // 调用第三方库的静态方法
        return SensitiveWordHelper.contains(text);
    }

    /**
     * 判断内容是否安全 (只是对上面方法的取反，方便业务层阅读)
     * @param text 待检测文本
     * @return true=安全, false=不安全
     */
    public boolean isSafe(String text) {
        return !hasSensitiveWord(text);
    }

    /**
     * 获取文本中的所有敏感词
     * @param text
     * @return 敏感词列表
     */
    public List<String> findAll(String text) {
        return SensitiveWordHelper.findAll(text);
    }
    
    /**
     * 替换敏感词（比如替换成 *）
     * @param text
     * @return 替换后的文本
     */
    public String replace(String text) {
        return SensitiveWordHelper.replace(text);
    }
}