package com.homecare.property.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房产实体
 */
@Data
@TableName("property")
public class Property {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属门店ID
     */
    private Long storeId;

    /**
     * 房东用户ID
     */
    private Long ownerId;

    /**
     * 房源标题
     */
    private String title;

    /**
     * 房源描述
     */
    private String description;

    /**
     * 房产类型：apartment/villa/office/shop
     */
    private String propertyType;

    /**
     * 租金（元/月）
     */
    private BigDecimal rentPrice;

    /**
     * 押金（元）
     */
    private BigDecimal deposit;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 所属区域
     */
    private String district;

    /**
     * 所属城市
     */
    private String city;

    /**
     * 面积（平方米）
     */
    private BigDecimal area;

    /**
     * 户型：如 "2室1厅1卫"
     */
    private String layout;

    /**
     * 当前楼层
     */
    private Integer floor;

    /**
     * 总楼层
     */
    private Integer totalFloor;

    /**
     * 朝向：north/south/east/west/northeast/northwest/southeast/southwest
     */
    private String orientation;

    /**
     * 配套设施（JSON数组）
     */
    private String facilities;

    /**
     * 状态：draft/published/rented/offline
     */
    private String status;

    /**
     * 标签（JSON数组）
     */
    private String tags;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
