package com.homecare.asset.controller;

import com.homecare.asset.common.Result;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.service.ProcurementProductService;
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
        Long storeId = getStoreId(httpRequest);
        ProcurementProductResponse response = productService.create(request, storeId);
        return Result.success("创建成功", response);
    }

    /**
     * 更新采购商品
     */
    @PutMapping("/{id}")
    public Result<ProcurementProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProcurementProductRequest request) {
        ProcurementProductResponse response = productService.update(id, request);
        return Result.success("更新成功", response);
    }

    /**
     * 获取采购商品详情
     */
    @GetMapping("/{id}")
    public Result<ProcurementProductResponse> getById(@PathVariable Long id) {
        ProcurementProductResponse response = productService.getById(id);
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
            @RequestParam(value = "status", required = false) String status) {
        var result = productService.list(page, pageSize, keyword, category, status);
        return Result.success(result);
    }

    /**
     * 删除采购商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新库存
     */
    @PatchMapping("/{id}/stock")
    public Result<ProcurementProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam("quantity") int quantity) {
        ProcurementProductResponse response = productService.updateStock(id, quantity);
        return Result.success("更新成功", response);
    }

    private Long getStoreId(HttpServletRequest request) {
        Object storeId = request.getAttribute("storeId");
        if (storeId == null) {
            return 1L;
        }
        return (Long) storeId;
    }
}
