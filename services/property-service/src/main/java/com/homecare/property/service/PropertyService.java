package com.homecare.property.service;

import com.homecare.property.common.PageResult;
import com.homecare.property.dto.PropertyCreateRequest;
import com.homecare.property.dto.PropertyResponse;
import com.homecare.property.dto.ViewingRequest;
import com.homecare.property.dto.ViewingResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房产服务接口
 */
public interface PropertyService {

    // ============ 房产管理 ============

    /**
     * 创建房产
     */
    PropertyResponse createProperty(PropertyCreateRequest request, Long userId);

    /**
     * 获取房产详情
     */
    PropertyResponse getPropertyById(Long id);

    /**
     * 更新房产
     */
    PropertyResponse updateProperty(Long id, PropertyCreateRequest request, Long userId);

    /**
     * 删除房产
     */
    void deleteProperty(Long id);

    /**
     * 分页查询房产列表
     */
    PageResult<PropertyResponse> listProperties(int page, int pageSize, String keyword,
            String propertyType, String district, BigDecimal minPrice, BigDecimal maxPrice,
            String status, Long ownerId);

    /**
     * 发布房产
     */
    PropertyResponse publishProperty(Long id);

    /**
     * 下架房产
     */
    PropertyResponse offlineProperty(Long id);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);

    /**
     * 推荐房产
     */
    PropertyResponse recommendProperty(Long id, boolean recommended);

    // ============ 预约看房 ============

    /**
     * 预约看房
     */
    ViewingResponse createViewing(ViewingRequest request, Long userId);

    /**
     * 获取预约详情
     */
    ViewingResponse getViewingById(Long id);

    /**
     * 确认预约
     */
    ViewingResponse confirmViewing(Long id);

    /**
     * 取消预约
     */
    ViewingResponse cancelViewing(Long id);

    /**
     * 完成看房
     */
    ViewingResponse completeViewing(Long id);

    /**
     * 分页查询预约列表
     */
    PageResult<ViewingResponse> listViewings(int page, int pageSize, Long propertyId,
            Long userId, String status);

    // ============ 图片管理 ============

    /**
     * 上传房产图片
     */
    PropertyResponse uploadImages(Long propertyId, List<String> imageUrls);
}
