package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.ReviewCreateRequest;
import com.homecare.serviceorder.dto.ReviewResponse;
import com.homecare.serviceorder.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping
    public Result<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ReviewResponse response = reviewService.createReview(request, userId);
        return Result.success("评价成功", response);
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{id}")
    public Result<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return Result.success(response);
    }

    /**
     * 分页查询评价
     */
    @GetMapping
    public Result<PageResult<ReviewResponse>> listReviews(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "staffId", required = false) Long staffId,
            @RequestParam(value = "storeId", required = false) Long storeId) {
        var result = reviewService.listReviews(page, pageSize, staffId, storeId);
        return Result.success(result);
    }

    /**
     * 获取订单评价
     */
    @GetMapping("/order/{orderId}")
    public Result<ReviewResponse> getReviewByOrderId(@PathVariable Long orderId) {
        ReviewResponse response = reviewService.getReviewByOrderId(orderId);
        return Result.success(response);
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return 1L;
        }
        return (Long) userId;
    }
}
