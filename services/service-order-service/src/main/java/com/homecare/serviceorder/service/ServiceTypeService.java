package com.homecare.serviceorder.service;

import com.homecare.serviceorder.dto.ServiceTypeCreateRequest;

import java.util.List;

/**
 * 服务类型接口
 */
public interface ServiceTypeService {

    /**
     * 服务类型列表（可选仅启用、关键词模糊匹配名称/描述）
     */
    List<?> listServiceTypes(boolean activeOnly, String keyword);

    /** 创建服务类型 */
    Object createServiceType(ServiceTypeCreateRequest request, String operatorRole);

    /** 更新服务类型 */
    Object updateServiceType(Long id, ServiceTypeCreateRequest request);

    /** 删除服务类型 */
    void deleteServiceType(Long id);

    /**
     * 根据ID获取服务类型（待上架审核仅平台管理员可见详情）
     */
    Object getServiceTypeById(Long id, Long viewerUserId, String viewerRole);

    /** 待上架审核的服务类型列表 */
    List<?> listPendingServiceTypes();

    Object approveServiceTypeListing(Long id);

    Object rejectServiceTypeListing(Long id);

    /** 重新提交上架审核 */
    Object submitServiceTypeListing(Long id);
}
