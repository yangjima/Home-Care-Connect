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
    PropertyResponse createProperty(PropertyCreateRequest request, Long userId, String role);

    /**
     * 获取房产详情（未登录仅可查看已上架空置房源；待审/驳回仅发布者或平台管理员）
     */
    PropertyResponse getPropertyById(Long id, Long viewerUserId, String viewerRole);

    /**
     * 更新房产
     */
    PropertyResponse updateProperty(Long id, PropertyCreateRequest request, Long userId, String role);

    /**
     * 删除房产
     */
    void deleteProperty(Long id, Long userId, String role);

    /**
     * 分页查询房产列表
     */
    PageResult<PropertyResponse> listProperties(int page, int pageSize, String keyword,
            String propertyType, String district, BigDecimal minPrice, BigDecimal maxPrice,
            List<String> propertyTypes, List<String> statuses, Long ownerId, String sort,
            List<String> facilities);

    /**
     * 发布房产（非平台管理员提交上架审核为待审；平台管理员可直接上架为空置）
     */
    PropertyResponse publishProperty(Long id, Long userId, String role);

    /**
     * 平台管理员通过房源上架审核
     */
    PropertyResponse approvePropertyListing(Long id, Long operatorUserId, String operatorRole);

    /**
     * 平台管理员驳回房源上架
     */
    PropertyResponse rejectPropertyListing(Long id, Long operatorUserId, String operatorRole);

    /**
     * 下架房产
     */
    PropertyResponse offlineProperty(Long id, Long userId, String role);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);

    /**
     * 推荐房产
     */
    PropertyResponse recommendProperty(Long id, boolean recommended, Long userId, String role);

    // ============ 预约看房 ============

    /**
     * 预约看房
     */
    ViewingResponse createViewing(ViewingRequest request, Long userId);

    /**
     * 获取预约详情
     */
    ViewingResponse getViewingById(Long id, Long operatorUserId, String operatorRole);

    /**
     * 确认预约
     */
    ViewingResponse confirmViewing(Long id, Long operatorUserId, String operatorRole);

    /**
     * 取消预约
     */
    ViewingResponse cancelViewing(Long id, Long operatorUserId, String operatorRole);

    /**
     * 完成看房
     */
    ViewingResponse completeViewing(Long id, Long operatorUserId, String operatorRole);

    /**
     * 分页查询预约列表
     */
    PageResult<ViewingResponse> listViewings(int page, int pageSize, Long propertyId,
            Long userId, String status, Long operatorUserId, String operatorRole);

    // ============ 图片管理 ============

    /**
     * 上传房产图片
     */
    PropertyResponse uploadImages(Long propertyId, List<String> imageUrls, Long userId, String role);
}
