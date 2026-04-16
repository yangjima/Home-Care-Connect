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
    private Long orderId;

    /**
     * 关联的房产ID
     */
    private Long propertyId;

    /**
     * 分销员ID
     */
    private Long distributorId;

    /**
     * 佣金金额
     */
    private BigDecimal amount;

    /**
     * 佣金比例
     */
    private BigDecimal rate;

    /**
     * 状态：pending/paid/cancelled
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
