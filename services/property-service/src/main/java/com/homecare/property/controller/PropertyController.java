package com.homecare.property.controller;

import com.homecare.property.common.Result;
import com.homecare.property.dto.PropertyCreateRequest;
import com.homecare.property.dto.PropertyResponse;
import com.homecare.property.service.PropertyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房产控制器
 */
@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * 创建房产
     */
    @PostMapping
    public Result<PropertyResponse> createProperty(
            @Valid @RequestBody PropertyCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        PropertyResponse response = propertyService.createProperty(request, userId);
        return Result.success("创建成功", response);
    }

    /**
     * 获取房产详情
     */
    @GetMapping("/{id}")
    public Result<PropertyResponse> getPropertyById(@PathVariable Long id) {
        // 增加浏览次数
        propertyService.incrementViewCount(id);
        PropertyResponse response = propertyService.getPropertyById(id);
        return Result.success(response);
    }

    /**
     * 更新房产
     */
    @PutMapping("/{id}")
    public Result<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        PropertyResponse response = propertyService.updateProperty(id, request, userId);
        return Result.success("更新成功", response);
    }

    /**
     * 删除房产
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProperty(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        // TODO: 权限检查
        propertyService.deleteProperty(id);
        return Result.success("删除成功", null);
    }

    /**
     * 分页查询房产列表
     */
    @GetMapping
    public Result<com.homecare.property.common.PageResult<PropertyResponse>> listProperties(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId) {

        var result = propertyService.listProperties(page, pageSize, keyword,
                propertyType, district, minPrice, maxPrice, status, ownerId);
        return Result.success(result);
    }

    /**
     * 发布房产
     */
    @PostMapping("/{id}/publish")
    public Result<PropertyResponse> publishProperty(@PathVariable Long id) {
        PropertyResponse response = propertyService.publishProperty(id);
        return Result.success("发布成功", response);
    }

    /**
     * 下架房产
     */
    @PostMapping("/{id}/offline")
    public Result<PropertyResponse> offlineProperty(@PathVariable Long id) {
        PropertyResponse response = propertyService.offlineProperty(id);
        return Result.success("下架成功", response);
    }

    /**
     * 推荐房产
     */
    @PostMapping("/{id}/recommend")
    public Result<PropertyResponse> recommendProperty(
            @PathVariable Long id,
            @RequestParam boolean recommended) {
        PropertyResponse response = propertyService.recommendProperty(id, recommended);
        return Result.success(recommended ? "推荐成功" : "取消推荐成功", response);
    }

    /**
     * 上传图片
     */
    @PostMapping("/{id}/images")
    public Result<PropertyResponse> uploadImages(
            @PathVariable Long id,
            @RequestBody List<String> imageUrls) {
        PropertyResponse response = propertyService.uploadImages(id, imageUrls);
        return Result.success("上传成功", response);
    }

    // ============ 辅助方法 ============

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            // 模拟开发时使用，线上通过 JWT Filter 注入
            return 1L;
        }
        return (Long) userId;
    }
}
