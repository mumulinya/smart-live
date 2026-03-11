package com.smartLive.interaction.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注关系实体对象
 * 用于存储用户对各类资源（店铺、博主等）的关注/订阅状态。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@TableName("follow")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Follow  implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @Excel(name = "用户id")
    private Long userId;

    /**
     * 来源类型  1（店铺）, 2（文章）, 3（团购）等。
     */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    /**
     * 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。
     */
    @Excel(name = "来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。")
    private Long sourceId;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Follow(Integer sourceType, Long sourceId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }
}
