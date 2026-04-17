package com.homecare.serviceorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务订单实体
 */
@Data
@TableName("service_order")
public class ServiceOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private Long storeId;

    @TableField("service_type_id")
    private Long serviceTypeId;

    @TableField("staff_id")
    private Long staffId;

    @TableField(exist = false)
    private Long propertyId;

    @TableField("address")
    private String serviceAddress;

    @TableField("appointment_time")
    private LocalDateTime serviceTime;

    @TableField(exist = false)
    private String duration;

    @TableField("price")
    private BigDecimal totalAmount;

    private String status;

    @TableField(exist = false)
    private String payStatus;

    @TableField(exist = false)
    private String payMethod;

    @TableField(exist = false)
    private LocalDateTime payTime;

    @TableField("description")
    private String remark;

    @TableField(exist = false)
    private String cancelReason;

    @TableField(exist = false)
    private LocalDateTime cancelTime;

    @TableField(value = "completed_at")
    private LocalDateTime completedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer deleted;
}
