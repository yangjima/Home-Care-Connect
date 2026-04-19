package com.homecare.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建房产请求
 */
@Data
public class PropertyCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "房产类型不能为空")
    private String propertyType;

    @NotNull(message = "租金不能为空")
    private BigDecimal rentPrice;

    private BigDecimal deposit;

    @NotBlank(message = "地址不能为空")
    private String address;

    private String district;
    private String city;

    @NotNull(message = "面积不能为空")
    private BigDecimal area;

    private String layout;
    private Integer floor;
    private Integer totalFloor;
    private String orientation;
    private List<String> facilities;
    private List<String> tags;

    /** 封面图片URL */
    private String coverImage;
    /** 图片URL列表 */
    private List<String> images;

    /** 视频URL列表 */
    private List<String> videos;

    /** 所属门店（超级管理员/商家发布时可指定；店长由服务端强制为本店） */
    private Long storeId;
}
