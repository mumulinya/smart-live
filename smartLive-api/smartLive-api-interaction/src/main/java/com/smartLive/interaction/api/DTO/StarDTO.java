package com.smartLive.interaction.api.DTO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注对象
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StarDTO   implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户id */
    private Long userId;

    /** 来源类型  1（文章）, 3（评论）, 4（代金券）等。 */
    private Integer sourceType;

    /** 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。 */
    private Long sourceId;

}
