package com.homecare.asset.controller;

import com.homecare.asset.common.Result;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.service.SecondhandItemService;
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
     * 发布二手商品
     */
    @PostMapping
    public Result<SecondhandItemResponse> create(
            @Valid @RequestBody SecondhandItemRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        SecondhandItemResponse response = itemService.create(request, userId);
        return Result.success("发布成功", response);
    }

    /**
     * 更新二手商品
     */
    @PutMapping("/{id}")
    public Result<SecondhandItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SecondhandItemRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        SecondhandItemResponse response = itemService.update(id, request, userId);
        return Result.success("更新成功", response);
    }

    /**
     * 获取二手商品详情
     */
    @GetMapping("/{id}")
    public Result<SecondhandItemResponse> getById(@PathVariable Long id) {
        itemService.incrementViewCount(id);
        SecondhandItemResponse response = itemService.getById(id);
        return Result.success(response);
    }

    /**
     * 分页查询二手商品
     */
    @GetMapping
    public Result<PageResult<SecondhandItemResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String status) {
        var result = itemService.list(page, pageSize, keyword, category, condition, status);
        return Result.success(result);
    }

    /**
     * 删除二手商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        itemService.delete(id, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取用户发布的商品
     */
    @GetMapping("/my")
    public Result<PageResult<SecondhandItemResponse>> listMyItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        var result = itemService.listByUserId(userId, page, pageSize);
        return Result.success(result);
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return 1L;
        }
        return (Long) userId;
    }
}
