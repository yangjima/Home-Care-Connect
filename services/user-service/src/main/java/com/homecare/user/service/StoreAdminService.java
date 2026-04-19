package com.homecare.user.service;

import com.homecare.user.dto.StoreCreateRequest;
import com.homecare.user.dto.StoreResponse;
import com.homecare.user.dto.StoreUpdateRequest;

import java.util.List;

public interface StoreAdminService {

    List<StoreResponse> listStores(String operatorRole, Long operatorStoreId);

    StoreResponse createStore(StoreCreateRequest request);

    StoreResponse updateStore(Long id, StoreUpdateRequest request);
}
