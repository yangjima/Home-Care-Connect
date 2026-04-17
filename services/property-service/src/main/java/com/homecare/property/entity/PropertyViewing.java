package com.homecare.property.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约看房实体
 */
@Data
@TableName("property_viewing")
public class PropertyViewing {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房产ID
     */
    @TableField("property_id")
    private Long propertyId;

    /**
     * 预约用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 分销员ID
     */
    @TableField("distributor_id")
    private Long distributorId;

    /**
     * 预约时间
     */
    @TableField("appointment_time")
    private LocalDateTime viewingTime;

    /**
     * 预约状态：pending/confirmed/completed/cancelled
     */
    private String status;

    /**
     * 用户备注
     */
    @TableField("remark")
    private String userRemark;

    /**
     * 管理员备注
     */
    @TableField(exist = false)
    private String adminRemark;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
