package com.smartLive.ai.entity.vo;

import lombok.Data;
import java.util.List;

/**
 * 博客生成视图对象。
 */
@Data
public class BlogGenerateVO {
    /** 3个候选标题 */
    private List<String> titles;
    /** 生成的正文 */
    private String content;
}
