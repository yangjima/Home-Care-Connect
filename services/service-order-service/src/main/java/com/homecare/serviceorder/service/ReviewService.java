package com.homecare.serviceorder.service;

import com.homecare.serviceorder.dto.ReviewCreateRequest;
import com.homecare.serviceorder.dto.ReviewResponse;
import com.homecare.serviceorder.common.PageResult;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 创建评价
     */
    ReviewResponse createReview(ReviewCreateRequest request, Long userId);

    /**
     * 获取评价详情
     */
    ReviewResponse getReviewById(Long id);

    /**
     * 分页查询评价
     */
    PageResult<ReviewResponse> listReviews(int page, int pageSize, Long staffId, Long storeId);

    /**
     * 获取订单评价
     */
    ReviewResponse getReviewByOrderId(Long orderId);

    /**
     * 获取员工平均评分
     */
    Double getStaffAverageRating(Long staffId);

    /**
     * 获取员工评价数量
     */
    int getStaffReviewCount(Long staffId);
}
