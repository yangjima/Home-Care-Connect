package com.homecare.user.service.impl;

import com.homecare.user.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 门店服务实现（占位实现，待 store-service 提供）
 */
@Slf4j
@Service
public class StoreServiceImpl implements StoreService {

    @Override
    public String getStoreName(Long storeId) {
        // TODO: 后续通过 Feign 调用 store-service 获取门店名称
        return "门店-" + storeId;
    }
}
