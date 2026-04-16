package com.homecare.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 采购商品创建请求
 */
@Data
public class ProcurementProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    private String category;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private String unit;

    private String image;

    private String images;
}
