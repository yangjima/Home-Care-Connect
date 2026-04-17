package com.homecare.serviceorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务类型实体
 */
@Data
@TableName("service_type")
public class ServiceType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("category")
    private String category;

    @TableField("base_price")
    private BigDecimal price;

    private String unit;

    private String icon;

    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer sortOrder;

    @TableField(exist = false)
    private Integer deleted;
}
