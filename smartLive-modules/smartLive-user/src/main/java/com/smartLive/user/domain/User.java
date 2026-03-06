package com.smartLive.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

/**
 * 用户对象 tb_user
 *
 * @author mumulin
 * @date 2025-09-21
 */
@TableName("user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    /** 密码，加密存储 */
    @Excel(name = "密码，加密存储")
    private String password;

    /** 昵称，默认是用户id */
    @Excel(name = "昵称，默认是用户id")
    private String nickName;

    /** 人物头像 */
    @Excel(name = "人物头像")
    private String icon;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("phone", getPhone())
            .append("password", getPassword())
            .append("nickName", getNickName())
            .append("icon", getIcon())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
