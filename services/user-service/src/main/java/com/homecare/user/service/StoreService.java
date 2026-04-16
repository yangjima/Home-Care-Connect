package com.homecare.user.service;

import com.homecare.user.common.PageResult;
import com.homecare.user.entity.User;

/**
 * 门店服务接口（通过Feign调用store-service）
 */
public interface StoreService {

    /**
     * 获取门店名称
     */
    String getStoreName(Long storeId);
}
