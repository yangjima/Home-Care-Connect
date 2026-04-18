package com.homecare.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品创建请求
 */
@Data
public class SecondhandItemRequest {

    private Long storeId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    private String category;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    /** 原价（可选） */
    private BigDecimal originalPrice;

    @NotBlank(message = "新旧程度不能为空")
    private String condition;

    private String contact;

    private String location;

    private String image;

    private String images;

    private LocalDateTime expireTime;
}
