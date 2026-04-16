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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 房产服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyViewingRepository propertyViewingRepository;
    private final ObjectMapper objectMapper;

    @Value("${minio.buckets.property:property-images}")
    private String propertyBucket;

    // ============ 房产管理 ============

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request, Long userId) {
        Property property = new Property();
        BeanUtils.copyProperties(request, property);
        property.setOwnerId(userId);
        property.setStatus("draft");
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

        // 保存图片
        saveImages(property.getId(), request.getImages(), request.getCoverImage());

        log.info("创建房产: id={}, title={}", property.getId(), property.getTitle());
        return getPropertyById(property.getId());
    }

    @Override
    public PropertyResponse getPropertyById(Long id) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        return toPropertyResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyCreateRequest request, Long userId) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }

        // 权限检查：只有房东可以修改
        if (!property.getOwnerId().equals(userId)) {
            throw new BusinessException(403, "无权修改此房产");
        }

        BeanUtils.copyProperties(request, property, "id", "ownerId", "storeId", "createTime");

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
        return getPropertyById(id);
    }

    @Override
    @Transactional
    public void deleteProperty(Long id) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
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
            String status, Long ownerId) {

        Page<Property> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Property::getDeleted, 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Property::getStatus, status);
        } else {
            // 默认只显示已发布的
            wrapper.eq(Property::getStatus, "published");
        }

        if (propertyType != null && !propertyType.isEmpty()) {
            wrapper.eq(Property::getPropertyType, propertyType);
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
        if (ownerId != null) {
            wrapper.eq(Property::getOwnerId, ownerId);
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

        wrapper.orderByDesc(Property::getIsRecommended)
                .orderByDesc(Property::getPublishedAt);

        Page<Property> result = propertyRepository.selectPage(pageParam, wrapper);
        List<PropertyResponse> records = result.getRecords().stream()
                .map(this::toPropertyResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, records);
    }

    @Override
    public PropertyResponse publishProperty(Long id) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        property.setStatus("published");
        property.setPublishedAt(LocalDateTime.now());
        propertyRepository.updateById(property);
        log.info("发布房产: id={}", id);
        return toPropertyResponse(property);
    }

    @Override
    public PropertyResponse offlineProperty(Long id) {
        Property property = propertyRepository.selectById(id);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        property.setStatus("offline");
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
    public PropertyResponse recommendProperty(Long id, boolean recommended) {
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

        PropertyViewing viewing = new PropertyViewing();
        viewing.setPropertyId(request.getPropertyId());
        viewing.setUserId(userId);
        viewing.setViewingTime(request.getViewingTime());
        viewing.setUserRemark(request.getRemark());
        viewing.setStatus("pending");

        propertyViewingRepository.insert(viewing);
        log.info("创建预约看房: id={}, propertyId={}, userId={}", viewing.getId(), request.getPropertyId(), userId);

        return getViewingById(viewing.getId());
    }

    @Override
    public ViewingResponse getViewingById(Long id) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse confirmViewing(Long id) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        viewing.setStatus("confirmed");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse cancelViewing(Long id) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        viewing.setStatus("cancelled");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public ViewingResponse completeViewing(Long id) {
        PropertyViewing viewing = propertyViewingRepository.selectById(id);
        if (viewing == null) {
            throw new BusinessException(404, "预约不存在");
        }
        viewing.setStatus("completed");
        propertyViewingRepository.updateById(viewing);
        return toViewingResponse(viewing);
    }

    @Override
    public PageResult<ViewingResponse> listViewings(int page, int pageSize, Long propertyId,
            Long userId, String status) {

        Page<PropertyViewing> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PropertyViewing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyViewing::getDeleted, 0);

        if (propertyId != null) {
            wrapper.eq(PropertyViewing::getPropertyId, propertyId);
        }
        if (userId != null) {
            wrapper.eq(PropertyViewing::getUserId, userId);
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
    public PropertyResponse uploadImages(Long propertyId, List<String> imageUrls) {
        Property property = propertyRepository.selectById(propertyId);
        if (property == null) {
            throw new BusinessException(404, "房产不存在");
        }
        saveImages(propertyId, imageUrls, null);
        return getPropertyById(propertyId);
    }

    // ============ 私有辅助方法 ============

    private void saveImages(Long propertyId, List<String> imageUrls, String coverImage) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            String type = (i == 0 || url.equals(coverImage)) ? "cover" : "room";

            PropertyImage image = new PropertyImage();
            image.setPropertyId(propertyId);
            image.setUrl(url);
            image.setType(type);
            image.setSortOrder(i);
            propertyImageRepository.insert(image);
        }
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
                .collect(Collectors.toList());
        response.setImages(imageUrls);

        // 设置封面
        String coverImage = images.stream()
                .filter(img -> "cover".equals(img.getType()))
                .map(PropertyImage::getUrl)
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
}
