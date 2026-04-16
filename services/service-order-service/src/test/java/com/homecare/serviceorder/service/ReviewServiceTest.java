package com.homecare.serviceorder.service;

import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.dto.ReviewCreateRequest;
import com.homecare.serviceorder.dto.ReviewResponse;
import com.homecare.serviceorder.entity.ServiceOrder;
import com.homecare.serviceorder.entity.ServiceReview;
import com.homecare.serviceorder.entity.ServiceStaff;
import com.homecare.serviceorder.repository.ServiceOrderRepository;
import com.homecare.serviceorder.repository.ServiceReviewRepository;
import com.homecare.serviceorder.repository.ServiceStaffRepository;
import com.homecare.serviceorder.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 评价服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ServiceReviewRepository reviewRepository;

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceStaffRepository staffRepository;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository, orderRepository, staffRepository
        );
    }

    @Nested
    @DisplayName("评价查询测试")
    class ReviewQueryTests {

        @Test
        @DisplayName("获取评价详情成功")
        void getReviewById_Success() {
            ServiceReview review = createTestReview(1L, 1L);
            when(reviewRepository.selectById(1L)).thenReturn(review);

            ReviewResponse response = reviewService.getReviewById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(5, response.getRating());
            assertEquals("服务很好", response.getContent());
        }

        @Test
        @DisplayName("获取评价详情-不存在")
        void getReviewById_NotFound() {
            when(reviewRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.getReviewById(999L));

            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("获取员工平均评分")
        void getStaffAverageRating_Success() {
            ServiceReview r1 = createTestReview(1L, 1L);
            r1.setRating(5);
            ServiceReview r2 = createTestReview(2L, 1L);
            r2.setRating(3);
            when(reviewRepository.selectList(any())).thenReturn(Arrays.asList(r1, r2));

            Double avg = reviewService.getStaffAverageRating(1L);

            assertEquals(4.0, avg);
        }

        @Test
        @DisplayName("获取员工平均评分-无评价")
        void getStaffAverageRating_NoReviews() {
            when(reviewRepository.selectList(any())).thenReturn(Arrays.asList());

            Double avg = reviewService.getStaffAverageRating(1L);

            assertEquals(0.0, avg);
        }
    }

    @Nested
    @DisplayName("评价创建测试")
    class ReviewCreateTests {

        @Test
        @DisplayName("创建评价成功")
        void createReview_Success() {
            ServiceOrder order = createTestOrder(1L, "completed");
            when(orderRepository.selectById(1L)).thenReturn(order);
            when(reviewRepository.selectCount(any())).thenReturn(0L);
            when(reviewRepository.insert(any())).thenReturn(1L);
            when(reviewRepository.selectList(any())).thenReturn(Arrays.asList());

            ServiceStaff staff = new ServiceStaff();
            staff.setId(1L);
            staff.setName("张三");
            staff.setStoreId(1L);
            when(staffRepository.selectById(1L)).thenReturn(staff);
            doNothing().when(staffRepository).updateById(any(ServiceStaff.class));

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setStaffId(1L);
            request.setRating(5);
            request.setContent("服务很好");

            ReviewResponse response = reviewService.createReview(request, 1L);

            assertNotNull(response);
            assertEquals(5, response.getRating());
            assertEquals("服务很好", response.getContent());
            verify(reviewRepository).insert(any(ServiceReview.class));
        }

        @Test
        @DisplayName("创建评价-订单不存在")
        void createReview_OrderNotFound() {
            when(orderRepository.selectById(999L)).thenReturn(null);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(999L);
            request.setRating(5);
            request.setContent("很好");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));

            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("创建评价-订单未完成")
        void createReview_OrderNotCompleted() {
            ServiceOrder order = createTestOrder(1L, "pending");
            when(orderRepository.selectById(1L)).thenReturn(order);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setRating(5);
            request.setContent("很好");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));

            assertEquals(400, exception.getCode());
        }

        @Test
        @DisplayName("创建评价-重复评价")
        void createReview_AlreadyReviewed() {
            ServiceOrder order = createTestOrder(1L, "completed");
            when(orderRepository.selectById(1L)).thenReturn(order);
            when(reviewRepository.selectCount(any())).thenReturn(1L);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setRating(5);
            request.setContent("很好");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));

            assertEquals(400, exception.getCode());
        }
    }

    // ============ 辅助方法 ============

    private ServiceReview createTestReview(Long id, Long staffId) {
        ServiceReview review = new ServiceReview();
        review.setId(id);
        review.setOrderId(1L);
        review.setUserId(1L);
        review.setStaffId(staffId);
        review.setStoreId(1L);
        review.setRating(5);
        review.setContent("服务很好");
        review.setIsAnonymous(0);
        review.setDeleted(0);
        review.setCreateTime(LocalDateTime.now());
        return review;
    }

    private ServiceOrder createTestOrder(Long id, String status) {
        ServiceOrder order = new ServiceOrder();
        order.setId(id);
        order.setOrderNo("SO2024041700001");
        order.setUserId(1L);
        order.setStoreId(1L);
        order.setServiceTypeId(1L);
        order.setStaffId(1L);
        order.setTotalAmount(new BigDecimal("200"));
        order.setStatus(status);
        order.setPayStatus("paid");
        order.setDeleted(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
