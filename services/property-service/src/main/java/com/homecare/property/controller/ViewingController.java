package com.homecare.property.controller;

import com.homecare.property.common.BusinessException;
import com.homecare.property.common.PageResult;
import com.homecare.property.common.Result;
import com.homecare.property.dto.ViewingRequest;
import com.homecare.property.dto.ViewingResponse;
import com.homecare.property.service.PropertyService;
import com.homecare.property.util.GatewayHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预约看房控制器
 */
@RestController
@RequestMapping("/viewings")
@RequiredArgsConstructor
public class ViewingController {

    private final PropertyService propertyService;

    /**
     * 预约看房
     */
    @PostMapping
    public Result<ViewingResponse> createViewing(
            @Valid @RequestBody ViewingRequest request,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        ViewingResponse response = propertyService.createViewing(request, userId);
        return Result.success("预约成功", response);
    }

    /**
     * 获取预约详情
     */
    @GetMapping("/{id}")
    public Result<ViewingResponse> getViewingById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        ViewingResponse response = propertyService.getViewingById(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success(response);
    }

    /**
     * 确认预约
     */
    @PostMapping("/{id}/confirm")
    public Result<ViewingResponse> confirmViewing(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        ViewingResponse response = propertyService.confirmViewing(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("确认成功", response);
    }

    /**
     * 取消预约
     */
    @PostMapping("/{id}/cancel")
    public Result<ViewingResponse> cancelViewing(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        ViewingResponse response = propertyService.cancelViewing(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("取消成功", response);
    }

    /**
     * 完成看房
     */
    @PostMapping("/{id}/complete")
    public Result<ViewingResponse> completeViewing(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        ViewingResponse response = propertyService.completeViewing(
                id, userId, role, GatewayHeaders.storeId(httpRequest));
        return Result.success("已完成", response);
    }

    /**
     * 分页查询预约列表
     */
    @GetMapping
    public Result<PageResult<ViewingResponse>> listViewings(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        Long operatorUserId = requireUserId(httpRequest);
        String operatorRole = GatewayHeaders.role(httpRequest);
        var result = propertyService.listViewings(page, pageSize, propertyId, userId, status,
                operatorUserId, operatorRole, GatewayHeaders.storeId(httpRequest));
        return Result.success(result);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = GatewayHeaders.userId(request);
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
