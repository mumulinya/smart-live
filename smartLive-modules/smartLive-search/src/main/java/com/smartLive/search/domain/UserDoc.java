package com.smartLive.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.lucene.document.FieldType;

/**
 * 用户索引文档对象
 * 对应 ES 中的 users 索引。用于实现在全站通过昵称、签名或地区搜索社交用户。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户数字 ID */
    private String id;

    /** 用户昵称 (支持模糊搜索) */
    private String nickName;

    /** 头像 URL */
    private String icon;

    /** 
     * 是否已被当前请求用户关注
     * 仅在搜索结果返回阶段根据 FollowService 实时填充，不存储在 ES 索引中。
     */
    private Boolean isFollow;

    /** 个人个性签名 (支持全文检索) */
    private String introduce;
    
    /** 常驻城市 */
    private String city;
}
