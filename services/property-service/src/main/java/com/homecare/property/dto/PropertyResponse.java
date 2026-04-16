package com.homecare.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房产响应
 */
@Data
public class PropertyResponse {

    private Long id;
    private Long storeId;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String propertyType;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private String address;
    private String district;
    private String city;
    private BigDecimal area;
    private String layout;
    private Integer floor;
    private Integer totalFloor;
    private String orientation;
    private List<String> facilities;
    private String status;
    private List<String> tags;
    private Integer viewCount;
    private Boolean isRecommended;
    private LocalDateTime publishedAt;
    private List<String> images;
    private String coverImage;
    private LocalDateTime createTime;
}
