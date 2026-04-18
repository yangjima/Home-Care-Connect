package com.homecare.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.property.common.BusinessException;
import com.homecare.property.common.PageResult;
import com.homecare.property.dto.*;
import com.homecare.property.entity.Property;
import com.homecare.property.entity.PropertyImage;
import com.homecare.property.entity.PropertyViewing;
import com.homecare.property.repository.PropertyImageRepository;
import com.homecare.property.repository.PropertyRepository;
import com.homecare.property.repository.PropertyViewingRepository;
import com.homecare.property.service.PropertyService;
import com.homecare.property.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 房产服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private static final Map<String, String> PROPERTY_TYPE_MAPPING = Map.ofEntries(
            Map.entry("普通住宅", "apartment"),
            Map.entry("公寓", "apartment"),
            Map.entry("整租公寓", "apartment"),
            Map.entry("洋房", "suite"),
            Map.entry("别墅", "villa"),
            Map.entry("单间", "studio"),
            Map.entry("套间", "suite"),
            Map.entry("一居", "studio"),
            Map.entry("apartment", "apartment"),
            Map.entry("studio", "studio"),
            Map.entry("suite", "suite"),
            Map.entry("villa", "villa")
    );

    private static final Map<String, String> STATUS_LABEL_MAPPING = Map.ofEntries(
            Map.entry("空置", "vacant"),
            Map.entry("已租", "occupied"),
            Map.entry("预定中", "reserved"),
            Map.entry("待审核", "pending"),
            Map.entry("待审", "pending"),
            Map.entry("已驳回", "rejected"),
            Map.entry("pending", "pending"),
            Map.entry("rejected", "rejected"),
            Map.entry("vacant", "vacant"),
            Map.entry("occupied", "occupied"),
            Map.entry("reserved", "reserved")
    );

    private static final Set<String> FACILITY_FILTER_WHITELIST = Set.of(
            "空调", "热水器", "洗衣机", "冰箱", "独立卫浴"
    );

    private static final Set<String> ALLOWED_SORT = Set.of(
            "comprehensive", "price_asc", "price_desc", "newest", "views"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogg", "mov", "m4v", "avi");

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyViewingRepository propertyViewingRepository;
    private final ObjectMapper objectMapper;

    @Value("${minio.buckets.property:property-images}")
    private String propertyBucket;

    // ============ 房产管理 ============

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request, Long userId, String role) {
        if (!Roles.canCreateProperty(role)) {
            throw new BusinessException(403, "无权发布房源");
        }
        Property property = new Property();
        BeanUtils.copyProperties(request, property);
        property.setPropertyType(normalizePropertyType(property.getPropertyType()));
        property.setOwnerId(userId);
        // 兼容前端未传 storeId 的场景，避免 DB 非空约束导致新增失败
        if (property.getStoreId() == null) {
            property.setStoreId(1L);
        }
        if (Roles.isPlatformAdmin(role)) {
            property.setStatus("vacant");
        } else {
            property.setStatus("pending");
        }
        property.setViewCount(0);
        property.setIsRecommended(false);

        // 序列化设施和标签为JSON
        try {
            if (request.getFacilities() != null) {
                property.setFacilities(objectMapper.writeValueAsString(request.getFacilities()));
            }
            if (request.getTags() != null) {
                property.setTags(objectMapper.writeValueAsString(request.getTags()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据序列化失败");
        }

        propertyRepository.insert(property);

        List<String> images = request.getImages() != null ? request.getImages() : Collections.emptyList();
        List<String> videos = request.getVideos() != null ? request.getVideos() : Collections.emptyList();
        replacePropertyMedia(property.getId(), images, videos, request.getCoverImage());

        log.info("创建房产: id={}, title={}", property.getId(), property.getTitle());
        return getPropertyById(property.getId(), userId, role);
    }

    @Override
    public PropertyResponse getPropertyById(Long id, Long viewerUserId, String viewerRole) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        assertPropertyReadable(property, viewerUserId, viewerRole);
        incrementViewCount(id);
        return toPropertyResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyCreateRequest request, Long userId, String role) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }

        assertPropertyOwnerOrAdmin(property, userId, role, "无权修改此房产");

        BeanUtils.copyProperties(request, property, "id", "ownerId", "storeId", "createTime");
        property.setPropertyType(normalizePropertyType(property.getPropertyType()));

        try {
            if (request.getFacilities() != null) {
                property.setFacilities(objectMapper.writeValueAsString(request.getFacilities()));
            }
            if (request.getTags() != null) {
                property.setTags(objectMapper.writeValueAsString(request.getTags()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据序列化失败");
        }

        propertyRepository.updateById(property);

        List<String> images = request.getImages() != null ? request.getImages() : Collections.emptyList();
        List<String> videos = request.getVideos() != null ? request.getVideos() : Collections.emptyList();
        replacePropertyMedia(id, images, videos, request.getCoverImage());

        return getPropertyById(id, userId, role);
    }

    @Override
    @Transactional
    public void deleteProperty(Long id, Long userId, String role) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        assertPropertyOwnerOrAdmin(property, userId, role, "无权删除此房产");
        propertyRepository.deleteById(id);
        // 图片也会被逻辑删除
        propertyImageRepository.delete(
                new LambdaQueryWrapper<PropertyImage>().eq(PropertyImage::getPropertyId, id)
        );
        log.info("删除房产: id={}", id);
    }

    @Override
    public PageResult<PropertyResponse> listProperties(int page, int pageSize, String keyword,
            String propertyType, String district, BigDecimal minPrice, BigDecimal maxPrice,
            List<String> propertyTypes, List<String> statuses, Long ownerId, String sort,
            List<String> facilities) {

        Page<Property> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();

        List<String> effectiveStatuses = normalizeStatusList(statuses);
        if (ownerId != null) {
            wrapper.eq(Property::getOwnerId, ownerId);
            if (!effectiveStatuses.isEmpty()) {
                wrapper.in(Property::getStatus, effectiveStatuses);
            }
        } else if (effectiveStatuses.isEmpty()) {
            wrapper.eq(Property::getStatus, "vacant");
        } else {
            wrapper.in(Property::getStatus, effectiveStatuses);
        }

        List<String> typeFilters = new ArrayList<>();
        if (propertyTypes != null) {
            for (String t : propertyTypes) {
                String n = mapPropertyTypeForFilter(t);
                if (n != null) {
                    typeFilters.add(n);
                }
            }
        }
        if (typeFilters.isEmpty() && propertyType != null && !propertyType.isEmpty()) {
            String n = mapPropertyTypeForFilter(propertyType);
            if (n != null) {
                typeFilters.add(n);
            }
        }
        if (!typeFilters.isEmpty()) {
            wrapper.in(Property::getPropertyType, typeFilters);
        }
        if (district != null && !district.isEmpty()) {
            wrapper.eq(Property::getDistrict, district);
        }
        if (minPrice != null) {
            wrapper.ge(Property::getRentPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Property::getRentPrice, maxPrice);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(Property::getTitle, keyword)
                    .or()
                    .like(Property::getDescription, keyword)
                    .or()
                    .like(Property::getAddress, keyword)
            );
        }

        applyFacilityFilters(wrapper, facilities);

        applySort(wrapper, sort);

        Page<Property> result = propertyRepository.selectPage(pageParam, wrapper);
        long totalRows = result.getTotal();
        if (totalRows <= 0 && !result.getRecords().isEmpty()) {
            totalRows = propertyRepository.selectCount(wrapper);
        }
        List<PropertyResponse> records = result.getRecords().stream()
                .map(this::toPropertyResponse)
                .collect(Collectors.toList());

        return PageResult.of(totalRows, page, pageSize, records);
    }

    @Override
    public PropertyResponse publishProperty(Long id, Long userId, String role) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        assertPropertyOwnerOrAdmin(property, userId, role, "无权上架此房源");
        if ("occupied".equals(property.getStatus())) {
            throw new BusinessException(400, "已出租房源不可申请上架");
        }
        if (Roles.isPlatformAdmin(role)) {
            property.setStatus("vacant");
        } else {
            property.setStatus("pending");
        }
        property.setPublishedAt(null);
        propertyRepository.updateById(property);
        log.info("发布房产: id={}, status={}", id, property.getStatus());
        return toPropertyResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse approvePropertyListing(Long id, Long operatorUserId, String operatorRole) {
        requirePlatformAdmin(operatorRole);
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        if (!"pending".equals(property.getStatus())) {
            throw new BusinessException(400, "仅待审核状态的房源可通过上架审批");
        }
        property.setStatus("vacant");
        propertyRepository.updateById(property);
        log.info("审批通过房源上架: id={}", id);
        return toPropertyResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse rejectPropertyListing(Long id, Long operatorUserId, String operatorRole) {
        requirePlatformAdmin(operatorRole);
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        if (!"pending".equals(property.getStatus())) {
            throw new BusinessException(400, "仅待审核状态的房源可驳回");
        }
        property.setStatus("rejected");
        propertyRepository.updateById(property);
        log.info("驳回房源上架: id={}", id);
        return toPropertyResponse(property);
    }

    @Override
    public PropertyResponse offlineProperty(Long id, Long userId, String role) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        assertPropertyOwnerOrAdmin(property, userId, role, "无权下架此房源");
        property.setStatus("reserved");
        propertyRepository.updateById(property);
        log.info("下架房产: id={}", id);
        return toPropertyResponse(property);
    }

    @Override
    public void incrementViewCount(Long id) {
        Property property = propertyRepository.selectById(id);
        if (property != null) {
            property.setViewCount(property.getViewCount() == null ? 1 : property.getViewCount() + 1);
            propertyRepository.updateById(property);
        }
    }

    @Override
    public PropertyResponse recommendProperty(Long id, boolean recommended, Long userId, String role) {
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅店长或超级管理员可设置推荐");
        }
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        property.setIsRecommended(recommended);
        propertyRepository.updateById(property);
        return toPropertyResponse(property);
    }

    // ============ 预约看房 ============

    @Override
    @Transactional
    public ViewingResponse createViewing(ViewingRequest request, Long userId) {
        Property property = propertyRepository.selectById(request.getPropertyId());
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        if (!"vacant".equals(property.getStatus())) {
            throw new BusinessException(400, "仅可预约已上架且空置中的房源");
        }

        PropertyViewing viewing = new PropertyViewing();
        viewing.setPropertyId(request.getPropertyId());
        viewing.setUserId(userId);
        viewing.setViewingTime(request.getViewingTime());
        viewing.setUserRemark(request.getRemark());
        viewing.setStatus("pending");

        propertyViewingRepository.insert(viewing);
        log.info("创建预约看房: id={}, propertyId={}, userId={}", viewing.getId(), request.getPropertyId(), userId);

        return getViewingById(viewing.getId(), userId, Roles.TENANT);
    }

    @Override
    public ViewingResponse getViewingById(Long id, Long operatorUserId, String operatorRole) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        assertCanReadViewing(viewing, operatorUserId, operatorRole);
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse confirmViewing(Long id, Long operatorUserId, String operatorRole) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        assertCanManageViewingAsOwner(viewing, operatorUserId, operatorRole);
        viewing.setStatus("confirmed");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse cancelViewing(Long id, Long operatorUserId, String operatorRole) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (Roles.isPlatformAdmin(operatorRole)) {
            // ok
        } else if (viewing.getUserId() != null && viewing.getUserId().equals(operatorUserId)) {
            // ok
        } else {
            assertCanManageViewingAsOwner(viewing, operatorUserId, operatorRole);
        }
        viewing.setStatus("cancelled");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse completeViewing(Long id, Long operatorUserId, String operatorRole) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        assertCanManageViewingAsOwner(viewing, operatorUserId, operatorRole);
        viewing.setStatus("completed");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public PageResult<ViewingResponse> listViewings(int page, int pageSize, Long propertyId,
            Long userId, String status, Long operatorUserId, String operatorRole) {

        Page<PropertyViewing> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PropertyViewing> wrapper = new LambdaQueryWrapper<>();

        Long effectivePropertyId = propertyId;
        Long effectiveUserId = userId;

        if (Roles.isPlatformAdmin(operatorRole)) {
            // 透传筛选
        } else if (Roles.isMerchant(operatorRole)) {
            effectiveUserId = null;
            if (effectivePropertyId == null) {
                throw new BusinessException(400, "商家查询预约需指定房源 propertyId");
            }
            Property p = propertyRepository.selectById(effectivePropertyId);
            if (p == null) {
                throw new BusinessException(404, "房源不存在");
            }
            assertPropertyOwnerOrAdmin(p, operatorUserId, operatorRole, "无权查看该房源的预约");
        } else {
            effectivePropertyId = null;
            effectiveUserId = operatorUserId;
        }

        if (effectivePropertyId != null) {
            wrapper.eq(PropertyViewing::getPropertyId, effectivePropertyId);
        }
        if (effectiveUserId != null) {
            wrapper.eq(PropertyViewing::getUserId, effectiveUserId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PropertyViewing::getStatus, status);
        }

        wrapper.orderByDesc(PropertyViewing::getCreateTime);
        Page<PropertyViewing> result = propertyViewingRepository.selectPage(pageParam, wrapper);

        List<ViewingResponse> records = result.getRecords().stream()
                .map(this::toViewingResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, records);
    }

    // ============ 图片管理 ============

    @Override
    @Transactional
    public PropertyResponse uploadImages(Long propertyId, List<String> imageUrls, Long userId, String role) {
        Property property = propertyRepository.selectById(propertyId);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        assertPropertyOwnerOrAdmin(property, userId, role, "无权更新该房源图片");
        List<PropertyImage> existing = propertyImageRepository.selectList(
                new LambdaQueryWrapper<PropertyImage>()
                        .eq(PropertyImage::getPropertyId, propertyId)
                        .orderByAsc(PropertyImage::getSortOrder));
        List<String> videos = existing.stream()
                .map(PropertyImage::getUrl)
                .filter(this::isVideoUrl)
                .collect(Collectors.toList());
        List<String> images = imageUrls != null ? imageUrls : Collections.emptyList();
        String cover = !images.isEmpty() ? images.get(0) : null;
        replacePropertyMedia(propertyId, images, videos, cover);
        return getPropertyById(propertyId, userId, role);
    }

    // ============ 私有辅助方法 ============

    private void requirePlatformAdmin(String role) {
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅店长或超级管理员可操作");
        }
    }

    /**
     * 未登录或普通访客仅可查看已上架（空置/已租）；待审、驳回、下架仅发布者或平台管理员。
     */
    private void assertPropertyReadable(Property property, Long viewerUserId, String viewerRole) {
        if (Roles.isPlatformAdmin(viewerRole)) {
            return;
        }
        if (viewerUserId != null && property.getOwnerId() != null && property.getOwnerId().equals(viewerUserId)) {
            return;
        }
        String st = property.getStatus();
        if ("vacant".equals(st) || "occupied".equals(st)) {
            return;
        }
        throw new BusinessException(404, "房产不存在");
    }

    private void assertPropertyOwnerOrAdmin(Property property, Long userId, String role, String message) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (Roles.isPlatformAdmin(role)) {
            return;
        }
        if (property.getOwnerId() != null && property.getOwnerId().equals(userId)) {
            return;
        }
        throw new BusinessException(403, message);
    }

    private void assertCanReadViewing(PropertyViewing viewing, Long operatorUserId, String operatorRole) {
        if (operatorUserId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (Roles.isPlatformAdmin(operatorRole)) {
            return;
        }
        if (viewing.getUserId() != null && viewing.getUserId().equals(operatorUserId)) {
            return;
        }
        Property p = propertyRepository.selectById(viewing.getPropertyId());
        if (p != null && p.getOwnerId() != null && p.getOwnerId().equals(operatorUserId)) {
            return;
        }
        throw new BusinessException(403, "无权查看此预约");
    }

    private void assertCanManageViewingAsOwner(PropertyViewing viewing, Long operatorUserId, String operatorRole) {
        if (operatorUserId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (Roles.isPlatformAdmin(operatorRole)) {
            return;
        }
        Property p = propertyRepository.selectById(viewing.getPropertyId());
        if (p != null && p.getOwnerId() != null && p.getOwnerId().equals(operatorUserId)) {
            return;
        }
        throw new BusinessException(403, "无权处理此预约");
    }

    /**
     * 用请求中的图片/视频列表全量替换该房源的媒体记录（封面仅针对图片 URL）。
     */
    private void replacePropertyMedia(Long propertyId, List<String> imageUrls, List<String> videoUrls,
            String coverImage) {
        propertyImageRepository.delete(
                new LambdaQueryWrapper<PropertyImage>().eq(PropertyImage::getPropertyId, propertyId));

        List<String> images = imageUrls != null ? imageUrls : Collections.emptyList();
        List<String> videos = videoUrls != null ? videoUrls : Collections.emptyList();
        long imageCount = images.stream().filter(url -> url != null && !url.isBlank()).count();
        long videoCount = videos.stream().filter(url -> url != null && !url.isBlank()).count();
        if (imageCount > 20) {
            throw new BusinessException(400, "房源图片最多 20 张");
        }
        if (videoCount > 3) {
            throw new BusinessException(400, "房源视频最多 3 个");
        }
        if (images.isEmpty() && videos.isEmpty()) {
            return;
        }

        int sortOrder = 0;
        String resolvedCover = coverImage != null && !coverImage.isBlank() ? coverImage.trim() : null;
        if (!images.isEmpty()) {
            String effectiveCover = resolvedCover;
            if (effectiveCover == null) {
                effectiveCover = trimUrl(images.get(0));
            } else {
                final String coverForMatch = effectiveCover;
                if (images.stream().noneMatch(u -> coverForMatch.equals(trimUrl(u)))) {
                    effectiveCover = trimUrl(images.get(0));
                }
            }
            for (String raw : images) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String url = raw.trim();
                PropertyImage row = new PropertyImage();
                row.setPropertyId(propertyId);
                row.setUrl(url);
                row.setType(effectiveCover != null && effectiveCover.equals(url) ? "cover" : "detail");
                row.setSortOrder(sortOrder++);
                propertyImageRepository.insert(row);
            }
        }
        for (String raw : videos) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String url = raw.trim();
            PropertyImage row = new PropertyImage();
            row.setPropertyId(propertyId);
            row.setUrl(url);
            row.setType("detail");
            row.setSortOrder(sortOrder++);
            propertyImageRepository.insert(row);
        }
    }

    private static String trimUrl(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private PropertyResponse toPropertyResponse(Property property) {
        PropertyResponse response = new PropertyResponse();
        BeanUtils.copyProperties(property, response);

        // 反序列化设施和标签
        try {
            if (property.getFacilities() != null) {
                response.setFacilities(objectMapper.readValue(
                        property.getFacilities(), new TypeReference<List<String>>() {}));
            }
            if (property.getTags() != null) {
                response.setTags(objectMapper.readValue(
                        property.getTags(), new TypeReference<List<String>>() {}));
            }
        } catch (JsonProcessingException e) {
            // 忽略解析错误
        }

        // 获取图片列表
        List<PropertyImage> images = propertyImageRepository.selectList(
                new LambdaQueryWrapper<PropertyImage>()
                        .eq(PropertyImage::getPropertyId, property.getId())
                        .orderByAsc(PropertyImage::getSortOrder)
        );
        List<String> imageUrls = images.stream()
                .map(PropertyImage::getUrl)
                .filter(url -> !isVideoUrl(url))
                .collect(Collectors.toList());
        response.setImages(imageUrls);

        List<String> videoUrls = images.stream()
                .map(PropertyImage::getUrl)
                .filter(this::isVideoUrl)
                .collect(Collectors.toList());
        response.setVideos(videoUrls);

        // 设置封面
        String coverImage = images.stream()
                .filter(img -> "cover".equals(img.getType()))
                .map(PropertyImage::getUrl)
                .filter(url -> !isVideoUrl(url))
                .findFirst()
                .orElse(imageUrls.isEmpty() ? null : imageUrls.get(0));
        response.setCoverImage(coverImage);

        return response;
    }

    private ViewingResponse toViewingResponse(PropertyViewing viewing) {
        ViewingResponse response = new ViewingResponse();
        BeanUtils.copyProperties(viewing, response);

        // 获取房产标题
        Property property = propertyRepository.selectById(viewing.getPropertyId());
        if (property != null) {
            response.setPropertyTitle(property.getTitle());
        }

        return response;
    }

    private List<String> normalizeStatusList(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return statuses.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String mapped = STATUS_LABEL_MAPPING.get(s);
                    if (mapped == null) {
                        mapped = STATUS_LABEL_MAPPING.get(s.toLowerCase(Locale.ROOT));
                    }
                    return mapped;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String mapPropertyTypeForFilter(String propertyType) {
        if (propertyType == null || propertyType.isBlank()) {
            return null;
        }
        String t = propertyType.trim();
        String normalized = PROPERTY_TYPE_MAPPING.get(t);
        if (normalized == null) {
            normalized = PROPERTY_TYPE_MAPPING.get(t.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private void applyFacilityFilters(LambdaQueryWrapper<Property> wrapper, List<String> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return;
        }
        for (String raw : facilities) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String f = raw.trim();
            if (!FACILITY_FILTER_WHITELIST.contains(f)) {
                continue;
            }
            wrapper.apply("JSON_CONTAINS(facilities, {0}, '$')", "\"" + f + "\"");
        }
    }

    private void applySort(LambdaQueryWrapper<Property> wrapper, String sort) {
        String s = (sort == null || sort.isBlank() || !ALLOWED_SORT.contains(sort))
                ? "comprehensive"
                : sort;
        switch (s) {
            case "price_asc":
                wrapper.orderByAsc(Property::getRentPrice).orderByDesc(Property::getId);
                break;
            case "price_desc":
                wrapper.orderByDesc(Property::getRentPrice).orderByDesc(Property::getId);
                break;
            case "newest":
                wrapper.orderByDesc(Property::getCreateTime);
                break;
            case "views":
                wrapper.orderByDesc(Property::getViewCount).orderByDesc(Property::getCreateTime);
                break;
            default:
                wrapper.orderByDesc(Property::getViewCount).orderByDesc(Property::getCreateTime);
                break;
        }
    }

    private String normalizePropertyType(String propertyType) {
        if (propertyType == null || propertyType.isBlank()) {
            throw new BusinessException("房产类型不能为空");
        }

        String normalized = PROPERTY_TYPE_MAPPING.get(propertyType.trim());
        if (normalized == null) {
            normalized = PROPERTY_TYPE_MAPPING.get(propertyType.trim().toLowerCase(Locale.ROOT));
        }
        if (normalized == null) {
            throw new BusinessException("不支持的房产类型: " + propertyType + "，可选值: apartment/studio/suite/villa");
        }
        return normalized;
    }

    private boolean isVideoUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String cleanUrl = url.split("\\?")[0];
        int dotIndex = cleanUrl.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanUrl.length() - 1) {
            return false;
        }
        String extension = cleanUrl.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.contains(extension);
    }
}
