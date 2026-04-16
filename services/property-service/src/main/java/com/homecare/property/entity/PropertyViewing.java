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
    private Long propertyId;

    /**
     * 预约用户ID
     */
    private Long userId;

    /**
     * 分销员ID
     */
    private Long distributorId;

    /**
     * 预约时间
     */
    private LocalDateTime viewingTime;

    /**
     * 预约状态：pending/confirmed/completed/cancelled
     */
    private String status;

    /**
     * 用户备注
     */
    private String userRemark;

    /**
     * 管理员备注
     */
    private String adminRemark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
