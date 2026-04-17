package com.homecare.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品实体
 */
@Data
@TableName("secondhand_item")
public class SecondhandItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("publisher_id")
    private Long userId;

    @TableField(exist = false)
    private Long storeId;

    private String title;

    private String description;

    private String category;

    private BigDecimal price;

    @TableField("`condition`")
    private String condition;

    @TableField(exist = false)
    private String image;

    private String images;

    private String status;

    @TableField(exist = false)
    private Long viewCount;

    @TableField(exist = false)
    private LocalDateTime expireTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer deleted;
}
