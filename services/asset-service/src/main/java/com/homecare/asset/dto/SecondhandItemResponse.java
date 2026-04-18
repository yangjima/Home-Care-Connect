package com.homecare.asset.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品响应
 */
@Data
public class SecondhandItemResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long storeId;
    private String storeName;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String condition;
    private String image;
    private String images;
    private String status;
    private Long viewCount;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    /** 是否展示「诚信」标签（由业务规则推导，原型展示用） */
    private Boolean integrityTag;

    private String location;
}
