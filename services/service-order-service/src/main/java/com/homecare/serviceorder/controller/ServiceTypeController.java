package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.dto.ServiceTypeCreateRequest;
import com.homecare.serviceorder.service.ServiceTypeService;
import com.homecare.serviceorder.util.GatewayHeaders;
import com.homecare.serviceorder.util.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务类型控制器
 */
@RestController
@RequestMapping("/service-types")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    /**
     * 获取服务类型列表
     */
    @GetMapping
    public Result<List<?>> listServiceTypes(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly,
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<?> types = serviceTypeService.listServiceTypes(activeOnly, keyword);
        return Result.success(types);
    }

    /**
     * 创建服务类型
     */
    @PostMapping
    public Result<Object> createServiceType(@Valid @RequestBody ServiceTypeCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        if (!Roles.isPlatformAdmin(role) && !Roles.isMerchant(role)) {
            throw new BusinessException(403, "仅商家、店长或超级管理员可创建服务");
        }
        Object created = serviceTypeService.createServiceType(request, role);
        String msg = Roles.isPlatformAdmin(role)
                ? "创建成功"
                : "已提交上架审核，请等待店长或超级管理员审批";
        return Result.success(msg, created);
    }

    /**
     * 更新服务类型
     */
    @PutMapping("/{id}")
    public Result<Object> updateServiceType(
            @PathVariable("id") Long id,
            @Valid @RequestBody ServiceTypeCreateRequest request,
            HttpServletRequest httpRequest) {
        requireManageRole(httpRequest);
        return Result.success("更新成功", serviceTypeService.updateServiceType(id, request));
    }

    /**
     * 删除服务类型
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteServiceType(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireManageRole(httpRequest);
        serviceTypeService.deleteServiceType(id);
        return Result.success("删除成功", null);
    }

    /**
     * 待上架审核的服务类型（平台管理员）
     */
    @GetMapping("/admin/pending")
    public Result<List<?>> listPendingServiceTypes(HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        return Result.success(serviceTypeService.listPendingServiceTypes());
    }

    /**
     * 通过服务类型上架审核（平台管理员）
     */
    @PostMapping("/{id}/approve-listing")
    public Result<Object> approveListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        return Result.success("已通过上架审核", serviceTypeService.approveServiceTypeListing(id));
    }

    /**
     * 驳回服务类型上架（平台管理员）
     */
    @PostMapping("/{id}/reject-listing")
    public Result<Object> rejectListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        return Result.success("已驳回上架申请", serviceTypeService.rejectServiceTypeListing(id));
    }

    /**
     * 重新提交上架审核
     */
    @PostMapping("/{id}/submit-listing")
    public Result<Object> submitListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireManageRole(httpRequest);
        return Result.success("已重新提交上架审核", serviceTypeService.submitServiceTypeListing(id));
    }

    /**
     * 获取服务类型详情
     */
    @GetMapping("/{id}")
    public Result<Object> getServiceType(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Object type = serviceTypeService.getServiceTypeById(
                id, GatewayHeaders.userId(httpRequest), GatewayHeaders.role(httpRequest));
        if (type == null) {
            return Result.notFound("服务类型不存在");
        }
        return Result.success(type);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = GatewayHeaders.userId(request);
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void requirePlatformAdmin(HttpServletRequest request) {
        requireUserId(request);
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅店长或超级管理员可操作");
        }
    }

    private void requireManageRole(HttpServletRequest request) {
        requireUserId(request);
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role) && !Roles.isMerchant(role)) {
            throw new BusinessException(403, "仅商家、店长或超级管理员可操作");
        }
    }
}
