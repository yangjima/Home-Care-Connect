package com.homecare.serviceorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.serviceorder.entity.ServiceType;
import com.homecare.serviceorder.repository.ServiceTypeRepository;
import com.homecare.serviceorder.service.ServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务类型实现
 */
@Service
@RequiredArgsConstructor
public class ServiceTypeServiceImpl implements ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Override
    public List<?> listAllServiceTypes() {
        return serviceTypeRepository.selectList(
                new LambdaQueryWrapper<ServiceType>()
                        .orderByAsc(ServiceType::getSortOrder)
        );
    }

    @Override
    public List<?> listActiveServiceTypes() {
        return serviceTypeRepository.selectList(
                new LambdaQueryWrapper<ServiceType>()
                        .eq(ServiceType::getStatus, "active")
                        .orderByAsc(ServiceType::getSortOrder)
        );
    }

    @Override
    public Object getServiceTypeById(Long id) {
        return serviceTypeRepository.selectById(id);
    }
}
