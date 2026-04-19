package com.homecare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.user.common.BusinessException;
import com.homecare.user.dto.StoreCreateRequest;
import com.homecare.user.dto.StoreResponse;
import com.homecare.user.dto.StoreUpdateRequest;
import com.homecare.user.entity.Store;
import com.homecare.user.repository.StoreRepository;
import com.homecare.user.service.StoreAdminService;
import com.homecare.user.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreAdminServiceImpl implements StoreAdminService {

    private final StoreRepository storeRepository;

    @Override
    public List<StoreResponse> listStores(String operatorRole, Long operatorStoreId) {
        if (Roles.ADMIN.equals(operatorRole)) {
            return storeRepository.selectList(new LambdaQueryWrapper<>()).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        if (Roles.STORE_MANAGER.equals(operatorRole)) {
            if (operatorStoreId == null) {
                return Collections.emptyList();
            }
            Store one = storeRepository.selectById(operatorStoreId);
            return one == null ? Collections.emptyList() : List.of(toResponse(one));
        }
        throw new BusinessException(403, "无权查看门店列表");
    }

    @Override
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Store s = new Store();
        s.setName(request.getName().trim());
        s.setAddress(request.getAddress().trim());
        s.setPhone(trimToNull(request.getPhone()));
        storeRepository.insert(s);
        log.info("创建门店 id={} name={}", s.getId(), s.getName());
        return toResponse(s);
    }

    @Override
    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request) {
        Store s = storeRepository.selectById(id);
        if (s == null) {
            throw new BusinessException(404, "门店不存在");
        }
        s.setName(request.getName().trim());
        s.setAddress(request.getAddress().trim());
        s.setPhone(trimToNull(request.getPhone()));
        storeRepository.updateById(s);
        return toResponse(s);
    }

    private static String trimToNull(String p) {
        if (p == null || p.isBlank()) {
            return null;
        }
        return p.trim();
    }

    private StoreResponse toResponse(Store s) {
        StoreResponse r = new StoreResponse();
        BeanUtils.copyProperties(s, r);
        return r;
    }
}
