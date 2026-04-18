package com.homecare.asset.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购商品响应
 */
@Data
public class ProcurementProductResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    /** 封面图（由 images JSON 解析或首图） */
    private String image;
    private String images;
    private Integer salesCount;
    private String productTag;
    private Long storeId;
    private String status;
    private LocalDateTime createTime;
}
