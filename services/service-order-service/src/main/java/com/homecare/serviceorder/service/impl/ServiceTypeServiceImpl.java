package com.homecare.serviceorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.dto.ServiceTypeCreateRequest;
import com.homecare.serviceorder.entity.ServiceType;
import com.homecare.serviceorder.repository.ServiceTypeRepository;
import com.homecare.serviceorder.service.ServiceTypeService;
import com.homecare.serviceorder.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 服务类型实现
 */
@Service
@RequiredArgsConstructor
public class ServiceTypeServiceImpl implements ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Override
    public List<?> listServiceTypes(boolean activeOnly, String keyword) {
        LambdaQueryWrapper<ServiceType> w = new LambdaQueryWrapper<>();
        if (activeOnly) {
            w.eq(ServiceType::getStatus, 1);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            w.and(q -> q.like(ServiceType::getName, k).or().like(ServiceType::getDescription, k));
        }
        w.orderByAsc(ServiceType::getId);
        return serviceTypeRepository.selectList(w);
    }

    @Override
    public Object createServiceType(ServiceTypeCreateRequest request, String operatorRole) {
        ServiceType type = new ServiceType();
        type.setName(request.getName().trim());
        type.setDescription(request.getDescription());
        type.setCategory(request.getCategory().trim());
        type.setPrice(request.getPrice());
        type.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit().trim() : "次");
        type.setIcon(request.getIcon());
        type.setStatus(Roles.isPlatformAdmin(operatorRole) ? 1 : 2);
        serviceTypeRepository.insert(type);
        return type;
    }

    @Override
    public Object updateServiceType(Long id, ServiceTypeCreateRequest request) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "服务类型不存在");
        }
        type.setName(request.getName().trim());
        type.setDescription(request.getDescription());
        type.setCategory(request.getCategory().trim());
        type.setPrice(request.getPrice());
        type.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit().trim() : "次");
        type.setIcon(request.getIcon());
        serviceTypeRepository.updateById(type);
        return type;
    }

    @Override
    public void deleteServiceType(Long id) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "服务类型不存在");
        }
        serviceTypeRepository.deleteById(id);
    }

    @Override
    public Object getServiceTypeById(Long id, Long viewerUserId, String viewerRole) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            return null;
        }
        if (type.getStatus() != null && type.getStatus() == 2 && !Roles.isPlatformAdmin(viewerRole)) {
            throw new BusinessException(404, "服务不存在");
        }
        return type;
    }

    @Override
    public List<?> listPendingServiceTypes() {
        LambdaQueryWrapper<ServiceType> w = new LambdaQueryWrapper<>();
        w.eq(ServiceType::getStatus, 2).orderByAsc(ServiceType::getId);
        return serviceTypeRepository.selectList(w);
    }

    @Override
    public Object approveServiceTypeListing(Long id) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "服务类型不存在");
        }
        if (type.getStatus() == null || type.getStatus() != 2) {
            throw new BusinessException(400, "仅待审核状态的服务可通过上架审批");
        }
        type.setStatus(1);
        serviceTypeRepository.updateById(type);
        return type;
    }

    @Override
    public Object rejectServiceTypeListing(Long id) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "服务类型不存在");
        }
        if (type.getStatus() == null || type.getStatus() != 2) {
            throw new BusinessException(400, "仅待审核状态的服务可驳回");
        }
        type.setStatus(0);
        serviceTypeRepository.updateById(type);
        return type;
    }

    @Override
    public Object submitServiceTypeListing(Long id) {
        ServiceType type = serviceTypeRepository.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "服务类型不存在");
        }
        if (type.getStatus() == null || type.getStatus() != 0) {
            throw new BusinessException(400, "仅下架状态可重新提交审核");
        }
        type.setStatus(2);
        serviceTypeRepository.updateById(type);
        return type;
    }
}
