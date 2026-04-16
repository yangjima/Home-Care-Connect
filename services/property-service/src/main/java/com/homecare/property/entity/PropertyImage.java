package com.homecare.property.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房产图片实体
 */
@Data
@TableName("property_image")
public class PropertyImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属房产ID
     */
    private Long propertyId;

    /**
     * 图片URL
     */
    private String url;

    /**
     * 图片类型：cover/room/kitchen/bathroom/other
     */
    private String type;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
