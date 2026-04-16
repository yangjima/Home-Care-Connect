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

    private String orderNo;

    private Long userId;

    private Long storeId;

    private Long serviceTypeId;

    private Long staffId;

    private Long propertyId;

    private String serviceAddress;

    private LocalDateTime serviceTime;

    private String duration;

    private BigDecimal totalAmount;

    private String status;

    private String payStatus;

    private String payMethod;

    private LocalDateTime payTime;

    private String remark;

    private String cancelReason;

    private LocalDateTime cancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
