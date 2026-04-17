package com.homecare.serviceorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("review")
public class ServiceReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private Long staffId;

    @TableField(exist = false)
    private Long storeId;

    private Integer rating;

    private String content;

    private String images;

    @TableField("target_type")
    private String targetType;

    @TableField(exist = false)
    private Integer isAnonymous;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer deleted;
}
