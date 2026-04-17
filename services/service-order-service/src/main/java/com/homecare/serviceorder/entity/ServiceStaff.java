package com.homecare.serviceorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务人员实体
 */
@Data
@TableName("service_staff")
public class ServiceStaff {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String phone;

    @TableField(exist = false)
    private String avatar;

    @TableField("store_id")
    private Long storeId;

    @TableField(exist = false)
    private Long serviceTypeId;

    private String status;

    @TableField("rating")
    private BigDecimal starRating;

    @TableField("total_orders")
    private Integer orderCount;

    @TableField("skills")
    private String skills;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer deleted;
}
