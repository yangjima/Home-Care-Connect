package com.homecare.serviceorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceTypeCreateRequest {

    @NotBlank(message = "服务名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "服务分类不能为空")
    private String category;

    @NotNull(message = "基础价格不能为空")
    private BigDecimal price;

    private String unit;

    private String icon;
}
