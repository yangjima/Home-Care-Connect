package com.homecare.serviceorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.ReviewCreateRequest;
import com.homecare.serviceorder.dto.ReviewResponse;
import com.homecare.serviceorder.entity.ServiceOrder;
import com.homecare.serviceorder.entity.ServiceReview;
import com.homecare.serviceorder.entity.ServiceStaff;
import com.homecare.serviceorder.repository.ServiceOrderRepository;
import com.homecare.serviceorder.repository.ServiceReviewRepository;
import com.homecare.serviceorder.repository.ServiceStaffRepository;
import com.homecare.serviceorder.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 评价服务实现
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ServiceReviewRepository reviewRepository;
    private final ServiceOrderRepository orderRepository;
    private final ServiceStaffRepository staffRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        ServiceOrder order = orderRepository.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new BusinessException(400, "只能评价已完成的订单");
        }

        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getOrderId, request.getOrderId());
        if (reviewRepository.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "该订单已评价");
        }

        ServiceReview review = new ServiceReview();
        review.setOrderId(request.getOrderId());
        review.setUserId(userId);
        review.setStaffId(request.getStaffId() != null ? request.getStaffId() : order.getStaffId());
        review.setStoreId(order.getStoreId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setImages(request.getImages());
        review.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : 0);

        reviewRepository.insert(review);

        updateStaffRating(review.getStaffId());

        return toResponse(review);
    }

    @Override
    public ReviewResponse getReviewById(Long id) {
        ServiceReview review = reviewRepository.selectById(id);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }
        return toResponse(review);
    }

    @Override
    public PageResult<ReviewResponse> listReviews(int page, int pageSize, Long staffId, Long storeId) {
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        if (staffId != null) wrapper.eq(ServiceReview::getStaffId, staffId);
        if (storeId != null) wrapper.eq(ServiceReview::getStoreId, storeId);
        wrapper.orderByDesc(ServiceReview::getCreateTime);

        Page<ServiceReview> pageResult = reviewRepository.selectPage(
                new Page<>(page, pageSize), wrapper);

        return new PageResult<>(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    public ReviewResponse getReviewByOrderId(Long orderId) {
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getOrderId, orderId);
        ServiceReview review = reviewRepository.selectOne(wrapper);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }
        return toResponse(review);
    }

    @Override
    public Double getStaffAverageRating(Long staffId) {
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getStaffId, staffId);
        var reviews = reviewRepository.selectList(wrapper);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double sum = reviews.stream().mapToInt(ServiceReview::getRating).sum();
        return sum / reviews.size();
    }

    @Override
    public int getStaffReviewCount(Long staffId) {
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getStaffId, staffId);
        return reviewRepository.selectCount(wrapper).intValue();
    }

    private void updateStaffRating(Long staffId) {
        if (staffId == null) return;
        ServiceStaff staff = staffRepository.selectById(staffId);
        if (staff == null) return;

        Double avgRating = getStaffAverageRating(staffId);
        int reviewCount = getStaffReviewCount(staffId);

        staff.setStarRating((int) Math.round(avgRating));
        staff.setOrderCount(reviewCount);
        staffRepository.updateById(staff);
    }

    private ReviewResponse toResponse(ServiceReview review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setUserId(review.getUserId());
        response.setStaffId(review.getStaffId());
        response.setStoreId(review.getStoreId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setImages(review.getImages());
        response.setIsAnonymous(review.getIsAnonymous());
        response.setCreateTime(review.getCreateTime());

        if (review.getStaffId() != null) {
            ServiceStaff staff = staffRepository.selectById(review.getStaffId());
            if (staff != null) {
                response.setStaffName(staff.getName());
            }
        }

        return response;
    }
}
