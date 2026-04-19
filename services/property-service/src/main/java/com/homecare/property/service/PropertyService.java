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

    PropertyResponse createProperty(PropertyCreateRequest request, Long userId, String role, Long operatorStoreId);

    PropertyResponse getPropertyById(Long id, Long viewerUserId, String viewerRole, Long viewerStoreId);

    PropertyResponse updateProperty(Long id, PropertyCreateRequest request, Long userId, String role, Long operatorStoreId);

    void deleteProperty(Long id, Long userId, String role, Long operatorStoreId);

    PageResult<PropertyResponse> listProperties(int page, int pageSize, String keyword,
            String propertyType, String district, BigDecimal minPrice, BigDecimal maxPrice,
            List<String> propertyTypes, List<String> statuses, Long ownerId, String sort,
            List<String> facilities, String listerRole, Long listerStoreId);

    PropertyResponse publishProperty(Long id, Long userId, String role, Long operatorStoreId);

    PropertyResponse approvePropertyListing(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    PropertyResponse rejectPropertyListing(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    PropertyResponse offlineProperty(Long id, Long userId, String role, Long operatorStoreId);

    void incrementViewCount(Long id);

    PropertyResponse recommendProperty(Long id, boolean recommended, Long userId, String role, Long operatorStoreId);

    ViewingResponse createViewing(ViewingRequest request, Long userId);

    ViewingResponse getViewingById(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    ViewingResponse confirmViewing(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    ViewingResponse cancelViewing(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    ViewingResponse completeViewing(Long id, Long operatorUserId, String operatorRole, Long operatorStoreId);

    PageResult<ViewingResponse> listViewings(int page, int pageSize, Long propertyId,
            Long userId, String status, Long operatorUserId, String operatorRole, Long operatorStoreId);

    PropertyResponse uploadImages(Long propertyId, List<String> imageUrls, Long userId, String role, Long operatorStoreId);
}
