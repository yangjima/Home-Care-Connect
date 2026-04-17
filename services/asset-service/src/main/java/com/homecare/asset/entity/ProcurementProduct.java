package com.homecare.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购商品实体
 */
@Data
@TableName("procurement_product")
public class ProcurementProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String category;

    private BigDecimal price;

    private Integer stock;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String image;

    private String images;

    @TableField("supplier_id")
    private Long storeId;

    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer deleted;
}
