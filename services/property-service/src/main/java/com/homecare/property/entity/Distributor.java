package com.homecare.property.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分销员实体
 */
@Data
@TableName("distributor")
public class Distributor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 所属门店ID
     */
    @TableField(exist = false)
    private Long storeId;

    /**
     * 真实姓名
     */
    @TableField(exist = false)
    private String realName;

    /**
     * 手机号
     */
    @TableField(exist = false)
    private String phone;

    @TableField("bind_code")
    private String bindCode;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("total_deals")
    private Integer totalDeals;

    @TableField("total_commission")
    private BigDecimal totalCommission;

    /**
     * 状态：1=启用 0=禁用
     */
    private Integer status;

    /**
     * 备注
     */
    @TableField(exist = false)
    private String remark;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
