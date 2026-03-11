package com.smartLive.search.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

/**
 * 用户资源聚合索引实体类
 * 该宽表索引用于存储用户与各业务实体（商铺、代金券、笔记等）之间的动态关系。
 * 支持高效检索“我收藏的 XXX”、“我点赞的 XXX”等社交化搜索场景。
 * 
 * @author smartLive
 * @date 2026-03-11
 * @param <T> 具体的业务文档实体 (如 ShopDoc, BlogDoc)
 */
@Data
public class UserResourceDoc<T> {

    /** 业务资源类型 (如：SHOP, VOUCHER, BLOG) */
    private String sourceType;
    
    /** 
     * 原始业务 ID 
     * 对应数据库各表的逻辑主键 (shopId, voucherId, blogId)
     */
    private Long sourceId;
    
    /** 
     * 动作标识符 
     * 枚举值见 UserResourceActionTypeConstants (FAVORITE: 收藏, LIKE: 点赞, HISTORY: 浏览历史)
     */
    private String actionType;
    
    /** 冗余存储的业务实体详细数据 (JSON 展开) */
    private T data;
}