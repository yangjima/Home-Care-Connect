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

    private Long userId;

    private Long storeId;

    private String title;

    private String description;

    private String category;

    private BigDecimal price;

    private String condition;

    private String image;

    private String images;

    private String status;

    private Long viewCount;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
