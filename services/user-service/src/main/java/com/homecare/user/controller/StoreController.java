package com.homecare.user.controller;

import com.homecare.user.common.BusinessException;
import com.homecare.user.common.Result;
import com.homecare.user.dto.StoreCreateRequest;
import com.homecare.user.dto.StoreResponse;
import com.homecare.user.dto.StoreUpdateRequest;
import com.homecare.user.service.StoreAdminService;
import com.homecare.user.util.GatewayHeaders;
import com.homecare.user.util.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店管理（创建/维护在超级管理员后台；店长仅可查看本店信息）
 */
@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreAdminService storeAdminService;

    @GetMapping
    public Result<List<StoreResponse>> list(HttpServletRequest request) {
        requirePlatformStaff(request);
        List<StoreResponse> list = storeAdminService.listStores(
                GatewayHeaders.role(request), GatewayHeaders.storeId(request));
        return Result.success(list);
    }

    @PostMapping
    public Result<StoreResponse> create(
            @Valid @RequestBody StoreCreateRequest body,
            HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("创建成功", storeAdminService.createStore(body));
    }

    @PutMapping("/{id}")
    public Result<StoreResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody StoreUpdateRequest body,
            HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("保存成功", storeAdminService.updateStore(id, body));
    }

    private static void requireAdmin(HttpServletRequest request) {
        String role = GatewayHeaders.role(request);
        if (!Roles.ADMIN.equals(role)) {
            throw new BusinessException(403, "仅超级管理员可维护门店");
        }
        if (GatewayHeaders.userId(request) == null) {
            throw new BusinessException(401, "未登录");
        }
    }

    private static void requirePlatformStaff(HttpServletRequest request) {
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "无权访问");
        }
        if (GatewayHeaders.userId(request) == null) {
            throw new BusinessException(401, "未登录");
        }
    }
}
