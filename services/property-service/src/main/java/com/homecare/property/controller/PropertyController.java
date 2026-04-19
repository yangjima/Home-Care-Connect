package com.homecare.property.controller;

import com.homecare.property.common.BusinessException;
import com.homecare.property.common.Result;
import com.homecare.property.dto.PropertyCreateRequest;
import com.homecare.property.dto.PropertyResponse;
import com.homecare.property.service.MinioService;
import com.homecare.property.service.PropertyService;
import com.homecare.property.service.VideoCompressionService;
import com.homecare.property.util.GatewayHeaders;
import com.homecare.property.util.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 房产控制器
 */
@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Slf4j
public class PropertyController {

    private static final long MAX_IMAGE_UPLOAD_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_UPLOAD_BYTES = 200L * 1024 * 1024;

    private final PropertyService propertyService;
    private final MinioService minioService;
    private final VideoCompressionService videoCompressionService;

    @Value("${minio.buckets.property:property-images}")
    private String propertyBucket;

    /**
     * 创建房产
     */
    @PostMapping
    public Result<PropertyResponse> createProperty(
            @Valid @RequestBody PropertyCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.createProperty(
                request, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("创建成功", response);
    }

    /**
     * 获取房产详情
     */
    @GetMapping("/{id}")
    public Result<PropertyResponse> getPropertyById(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        PropertyResponse response = propertyService.getPropertyById(
                id,
                GatewayHeaders.userId(httpRequest),
                GatewayHeaders.role(httpRequest),
                GatewayHeaders.storeId(httpRequest));
        return Result.success(response);
    }

    /**
     * 更新房产
     */
    @PutMapping("/{id}")
    public Result<PropertyResponse> updateProperty(
            @PathVariable("id") Long id,
            @Valid @RequestBody PropertyCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.updateProperty(
                id, request, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("更新成功", response);
    }

    /**
     * 删除房产
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProperty(
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        propertyService.deleteProperty(id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("删除成功", null);
    }

    /**
     * 分页查询房产列表
     */
    @GetMapping
    public Result<com.homecare.property.common.PageResult<PropertyResponse>> listProperties(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "types", required = false) String types,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "status", required = false) String legacyStatus,
            @RequestParam(value = "facilities", required = false) String facilities,
            @RequestParam(value = "sort", defaultValue = "comprehensive") String sort,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            HttpServletRequest httpRequest) {

        List<String> statusList = splitComma(statuses);
        if (statusList.isEmpty() && StringUtils.hasText(legacyStatus)) {
            statusList = Collections.singletonList(legacyStatus.trim());
        }

        var result = propertyService.listProperties(page, pageSize, keyword,
                propertyType, district, minPrice, maxPrice,
                splitComma(types), statusList, ownerId, sort, splitComma(facilities),
                GatewayHeaders.role(httpRequest), GatewayHeaders.storeId(httpRequest));
        return Result.success(result);
    }

    /**
     * 发布房产
     */
    @PostMapping("/{id}/publish")
    public Result<PropertyResponse> publishProperty(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.publishProperty(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        String msg = (Roles.isSuperAdmin(role) || Roles.isStoreManager(role))
                ? "上架成功"
                : "已提交上架审核，请等待店长或超级管理员审批";
        return Result.success(msg, response);
    }

    /**
     * 审批通过房源上架（平台管理员）
     */
    @PostMapping("/{id}/approve-listing")
    public Result<PropertyResponse> approveListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.approvePropertyListing(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("已通过上架审核", response);
    }

    /**
     * 驳回房源上架（平台管理员）
     */
    @PostMapping("/{id}/reject-listing")
    public Result<PropertyResponse> rejectListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.rejectPropertyListing(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("已驳回上架申请", response);
    }

    /**
     * 下架房产
     */
    @PostMapping("/{id}/offline")
    public Result<PropertyResponse> offlineProperty(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.offlineProperty(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("下架成功", response);
    }

    /**
     * 推荐房产
     */
    @PostMapping("/{id}/recommend")
    public Result<PropertyResponse> recommendProperty(
            @PathVariable("id") Long id,
            @RequestParam("recommended") boolean recommended,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.recommendProperty(
                id, recommended, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success(recommended ? "推荐成功" : "取消推荐成功", response);
    }

    /**
     * 上传图片
     */
    @PostMapping("/{id}/images")
    public Result<PropertyResponse> uploadImages(
            @PathVariable("id") Long id,
            @RequestBody List<String> imageUrls,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        PropertyResponse response = propertyService.uploadImages(
                id, imageUrls, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("上传成功", response);
    }

    /**
     * 上传房源媒体（图片/视频）
     */
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadMedia(@RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            return Result.badRequest("仅支持上传图片或视频文件");
        }

        long size = file.getSize();
        boolean isVideo = contentType.startsWith("video/");
        if (!isVideo && size > MAX_IMAGE_UPLOAD_BYTES) {
            return Result.badRequest("图片单张不能超过 20MB");
        }
        if (isVideo && size > MAX_VIDEO_UPLOAD_BYTES) {
            return Result.badRequest("视频单文件不能超过 200MB");
        }

        Long userId = requireUserId(httpRequest);
        String mediaType = isVideo ? "video" : "image";
        String extension;
        byte[] body;
        String uploadContentType;
        try {
            if (isVideo) {
                VideoCompressionService.PreparedVideo prepared = videoCompressionService.prepareForStorage(file);
                body = prepared.data();
                uploadContentType = prepared.contentType();
                extension = prepared.filenameExtension();
                if (!StringUtils.hasText(extension)) {
                    extension = getFileExtension(file.getOriginalFilename());
                }
                if (!extension.startsWith(".")) {
                    extension = "." + extension;
                }
            } else {
                body = file.getBytes();
                uploadContentType = contentType;
                extension = getFileExtension(file.getOriginalFilename());
            }
        } catch (Exception e) {
            return Result.error(500, "读取上传文件失败: " + e.getMessage());
        }

        String objectName = "property/" + userId + "/" + mediaType + "/"
                + UUID.randomUUID().toString().replace("-", "") + extension;

        String url;
        try {
            url = minioService.uploadFile(propertyBucket, objectName, body, uploadContentType);
        } catch (Exception e) {
            return Result.error(500, "媒体上传失败: " + e.getMessage());
        }

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("mediaType", mediaType);
        return Result.success("上传成功", data);
    }

    // ============ 辅助方法 ============

    private static List<String> splitComma(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = GatewayHeaders.userId(request);
        if (userId == null) {
            log.warn("未获取到用户ID，请检查网关是否注入 X-User-Id");
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index);
    }
}
