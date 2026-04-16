package com.homecare.serviceorder.service;

import java.util.List;

/**
 * 服务类型接口
 */
public interface ServiceTypeService {

    /**
     * 获取所有服务类型
     */
    List<?> listAllServiceTypes();

    /**
     * 获取启用的服务类型
     */
    List<?> listActiveServiceTypes();

    /**
     * 根据ID获取服务类型
     */
    Object getServiceTypeById(Long id);
}
