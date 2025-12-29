package com.smartLive.ai.entity.DOC;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户对象 tb_user
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class UserDoc
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 昵称，默认是用户id */
    private String nickName;

    /** 人物头像 */
    private String icon;

    private Boolean isFollow;


    /** 个性签名 */
    private String introduce;
    /** 城市 */
    private String city;
    private Date createTime;

}
