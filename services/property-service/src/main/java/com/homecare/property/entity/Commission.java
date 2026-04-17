package com.homecare.property.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金记录实体
 */
@Data
@TableName("commission")
public class Commission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的订单ID（服务订单或租赁）
     */
    @TableField(exist = false)
    private Long orderId;

    /**
     * 关联的房产ID
     */
    @TableField("property_id")
    private Long propertyId;

    /**
     * 分销员ID
     */
    @TableField("distributor_id")
    private Long distributorId;

    /**
     * 佣金金额
     */
    @TableField("commission_amount")
    private BigDecimal amount;

    /**
     * 佣金比例
     */
    @TableField("commission_rate")
    private BigDecimal rate;

    @TableField("deal_price")
    private BigDecimal dealPrice;

    /**
     * 状态：pending/settled/cancelled
     */
    private String status;

    /**
     * 备注
     */
    @TableField(exist = false)
    private String remark;

    @TableField(value = "settled_at")
    private LocalDateTime settledAt;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
