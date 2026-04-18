package com.homecare.asset.controller;

import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.Result;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.dto.SecondhandMarketStatsResponse;
import com.homecare.asset.service.SecondhandItemService;
import com.homecare.asset.util.GatewayHeaders;
import com.homecare.asset.util.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 二手商品控制器
 */
@RestController
@RequestMapping("/secondhand-items")
@RequiredArgsConstructor
public class SecondhandItemController {

    private final SecondhandItemService itemService;

    /**
     * 跳蚤市场页统计（在售总数、本周新增）
     */
    @GetMapping("/summary")
    public Result<SecondhandMarketStatsResponse> summary() {
        return Result.success(itemService.getMarketStats());
    }

    /**
     * 发布二手商品
     */
    @PostMapping
    public Result<SecondhandItemResponse> create(
            @Valid @RequestBody SecondhandItemRequest request,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        SecondhandItemResponse response = itemService.create(request, userId, role);
        String message = Roles.isPlatformAdmin(role)
                ? "发布成功，商品已上架"
                : "已提交上架审核，请等待店长或超级管理员审批";
        return Result.success(message, response);
    }

    /**
     * 更新二手商品
     */
    @PutMapping("/{id:\\d+}")
    public Result<SecondhandItemResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody SecondhandItemRequest request,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        SecondhandItemResponse response = itemService.update(id, request, userId);
        return Result.success("更新成功", response);
    }

    /**
     * 获取二手商品详情
     */
    @GetMapping("/{id:\\d+}")
    public Result<SecondhandItemResponse> getById(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        SecondhandItemResponse response = itemService.getById(
                id, GatewayHeaders.userId(httpRequest), GatewayHeaders.role(httpRequest));
        return Result.success(response);
    }

    /**
     * 分页查询二手商品
     */
    @GetMapping
    public Result<PageResult<SecondhandItemResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        var result = itemService.list(page, pageSize, keyword, category, condition, status,
                GatewayHeaders.userId(httpRequest), GatewayHeaders.role(httpRequest));
        return Result.success(result);
    }

    /**
     * 通过跳蚤商品上架审核（平台管理员）
     */
    @PostMapping("/{id:\\d+}/approve-listing")
    public Result<SecondhandItemResponse> approveListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        SecondhandItemResponse response = itemService.approveListing(id, GatewayHeaders.role(httpRequest));
        return Result.success("已通过上架审核", response);
    }

    /**
     * 驳回跳蚤商品上架（平台管理员）
     */
    @PostMapping("/{id:\\d+}/reject-listing")
    public Result<SecondhandItemResponse> rejectListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        SecondhandItemResponse response = itemService.rejectListing(id, GatewayHeaders.role(httpRequest));
        return Result.success("已驳回上架申请", response);
    }

    /**
     * 重新提交上架审核（发布者）
     */
    @PostMapping("/{id:\\d+}/submit-listing")
    public Result<SecondhandItemResponse> submitListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        SecondhandItemResponse response = itemService.submitForListing(id, userId);
        return Result.success("已重新提交上架审核", response);
    }

    /**
     * 删除二手商品
     */
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        itemService.delete(id, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取用户发布的商品
     */
    @GetMapping("/my")
    public Result<PageResult<SecondhandItemResponse>> listMyItems(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        var result = itemService.listByUserId(userId, page, pageSize);
        return Result.success(result);
    }

    private Long requireUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header != null && !header.isBlank()) {
            try {
                return Long.parseLong(header.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId != null) {
            try {
                return Long.parseLong(userId.toString());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw new BusinessException(401, "请先登录");
    }

    private void requirePlatformAdmin(HttpServletRequest request) {
        requireUserId(request);
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅店长或超级管理员可审批上架");
        }
    }
}
