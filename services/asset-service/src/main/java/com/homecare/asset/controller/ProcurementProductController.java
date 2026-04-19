package com.homecare.asset.controller;

import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.Result;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.ProcurementMallStatsResponse;
import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.service.ProcurementProductService;
import com.homecare.asset.util.GatewayHeaders;
import com.homecare.asset.util.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购商品控制器
 */
@RestController
@RequestMapping("/procurement-products")
@RequiredArgsConstructor
public class ProcurementProductController {

    private final ProcurementProductService productService;

    /**
     * 创建采购商品
     */
    @PostMapping
    public Result<ProcurementProductResponse> create(
            @Valid @RequestBody ProcurementProductRequest request,
            HttpServletRequest httpRequest) {
        requireProcurementManager(httpRequest);
        Long storeId = 1L;
        String role = GatewayHeaders.role(httpRequest);
        ProcurementProductResponse response = productService.create(request, storeId, role);
        String msg = Roles.isPlatformAdmin(role)
                ? "创建成功" : "已提交上架审核，请等待店长或超级管理员审批";
        return Result.success(msg, response);
    }

    /**
     * 更新采购商品
     */
    @PutMapping("/{id}")
    public Result<ProcurementProductResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProcurementProductRequest request,
            HttpServletRequest httpRequest) {
        requireProcurementManager(httpRequest);
        ProcurementProductResponse response = productService.update(id, request);
        return Result.success("更新成功", response);
    }

    /**
     * 本地商城首页统计（已上架商品总数）
     */
    @GetMapping("/summary")
    public Result<ProcurementMallStatsResponse> mallStats() {
        return Result.success(productService.getMallStats());
    }

    /**
     * 获取采购商品详情
     */
    @GetMapping("/{id}")
    public Result<ProcurementProductResponse> getById(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        ProcurementProductResponse response = productService.getById(
                id, GatewayHeaders.userId(httpRequest), GatewayHeaders.role(httpRequest));
        return Result.success(response);
    }

    /**
     * 分页查询采购商品
     */
    @GetMapping
    public Result<PageResult<ProcurementProductResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort,
            HttpServletRequest httpRequest) {
        var result = productService.list(page, pageSize, keyword, category, status, sort,
                GatewayHeaders.userId(httpRequest), GatewayHeaders.role(httpRequest));
        return Result.success(result);
    }

    /**
     * 通过采购商品上架审核（平台管理员）
     */
    @PostMapping("/{id}/approve-listing")
    public Result<ProcurementProductResponse> approveListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        ProcurementProductResponse response = productService.approveListing(id, GatewayHeaders.role(httpRequest));
        return Result.success("已通过上架审核", response);
    }

    /**
     * 驳回采购商品上架（平台管理员）
     */
    @PostMapping("/{id}/reject-listing")
    public Result<ProcurementProductResponse> rejectListing(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        ProcurementProductResponse response = productService.rejectListing(id, GatewayHeaders.role(httpRequest));
        return Result.success("已驳回上架申请", response);
    }

    /**
     * 删除采购商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireProcurementManager(httpRequest);
        productService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新库存
     */
    @PatchMapping("/{id}/stock")
    public Result<ProcurementProductResponse> updateStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity,
            HttpServletRequest httpRequest) {
        requireProcurementManager(httpRequest);
        ProcurementProductResponse response = productService.updateStock(id, quantity);
        return Result.success("更新成功", response);
    }

    private void requireProcurementManager(HttpServletRequest request) {
        if (GatewayHeaders.userId(request) == null) {
            throw new BusinessException(401, "未登录");
        }
        String role = GatewayHeaders.role(request);
        if (!Roles.canManageProcurement(role)) {
            throw new BusinessException(403, "仅商家或店长可管理本地商城商品");
        }
    }

    private void requirePlatformAdmin(HttpServletRequest request) {
        if (GatewayHeaders.userId(request) == null) {
            throw new BusinessException(401, "未登录");
        }
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅店长或超级管理员可审批上架");
        }
    }
}
