package com.homecare.serviceorder.service;

import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.OrderCreateRequest;
import com.homecare.serviceorder.dto.OrderResponse;
import com.homecare.serviceorder.entity.ServiceOrder;
import com.homecare.serviceorder.entity.ServiceType;
import com.homecare.serviceorder.repository.ServiceOrderRepository;
import com.homecare.serviceorder.repository.ServiceTypeRepository;
import com.homecare.serviceorder.repository.ServiceStaffRepository;
import com.homecare.serviceorder.service.impl.ServiceOrderServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 服务订单服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @Mock
    private ServiceStaffRepository staffRepository;

    private ServiceOrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new ServiceOrderServiceImpl(
                orderRepository, serviceTypeRepository, staffRepository
        );
    }

    @Nested
    @DisplayName("订单查询测试")
    class OrderQueryTests {

        @Test
        @DisplayName("获取订单详情成功")
        void getOrderById_Success() {
            ServiceOrder order = createTestOrder(1L, "pending", "unpaid");
            when(orderRepository.selectById(1L)).thenReturn(order);

            OrderResponse response = orderService.getOrderById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("pending", response.getStatus());
            assertEquals("unpaid", response.getPayStatus());
        }

        @Test
        @DisplayName("获取订单详情-订单不存在")
        void getOrderById_NotFound() {
            when(orderRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.getOrderById(999L));

            assertEquals(404, exception.getCode());
            assertEquals("订单不存在", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("订单状态测试")
    class OrderStatusTests {

        @Test
        @DisplayName("确认订单成功")
        void confirmOrder_Success() {
            ServiceOrder order = createTestOrder(1L, "pending", "paid");
            when(orderRepository.selectById(1L)).thenReturn(order);
            doNothing().when(orderRepository).updateById(any(ServiceOrder.class));

            OrderResponse response = orderService.confirmOrder(1L);

            assertNotNull(response);
            verify(orderRepository).updateById(argThat(o ->
                    "confirmed".equals(o.getStatus())
            ));
        }

        @Test
        @DisplayName("确认订单-非待确认状态")
        void confirmOrder_InvalidStatus() {
            ServiceOrder order = createTestOrder(1L, "confirmed", "paid");
            when(orderRepository.selectById(1L)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.confirmOrder(1L));

            assertEquals(400, exception.getCode());
        }

        @Test
        @DisplayName("完成订单成功")
        void completeOrder_Success() {
            ServiceOrder order = createTestOrder(1L, "confirmed", "paid");
            when(orderRepository.selectById(1L)).thenReturn(order);
            doNothing().when(orderRepository).updateById(any(ServiceOrder.class));

            OrderResponse response = orderService.completeOrder(1L);

            assertNotNull(response);
            verify(orderRepository).updateById(argThat(o ->
                    "completed".equals(o.getStatus())
            ));
        }

        @Test
        @DisplayName("取消订单成功")
        void cancelOrder_Success() {
            ServiceOrder order = createTestOrder(1L, "pending", "unpaid");
            when(orderRepository.selectById(1L)).thenReturn(order);
            doNothing().when(orderRepository).updateById(any(ServiceOrder.class));

            OrderResponse response = orderService.cancelOrder(1L, "时间冲突", 1L);

            assertNotNull(response);
            verify(orderRepository).updateById(argThat(o ->
                    "cancelled".equals(o.getStatus()) &&
                            "时间冲突".equals(o.getCancelReason())
            ));
        }

        @Test
        @DisplayName("取消订单-已完成状态无法取消")
        void cancelOrder_InvalidStatus() {
            ServiceOrder order = createTestOrder(1L, "completed", "paid");
            when(orderRepository.selectById(1L)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(1L, "原因", 1L));

            assertEquals(400, exception.getCode());
        }
    }

    @Nested
    @DisplayName("订单支付测试")
    class OrderPayTests {

        @Test
        @DisplayName("支付订单成功")
        void payOrder_Success() {
            ServiceOrder order = createTestOrder(1L, "pending", "unpaid");
            when(orderRepository.selectById(1L)).thenReturn(order);
            doNothing().when(orderRepository).updateById(any(ServiceOrder.class));

            OrderResponse response = orderService.payOrder(1L, "wechat");

            assertNotNull(response);
            verify(orderRepository).updateById(argThat(o ->
                    "paid".equals(o.getPayStatus()) &&
                            "wechat".equals(o.getPayMethod()) &&
                            o.getPayTime() != null
            ));
        }

        @Test
        @DisplayName("支付订单-已支付")
        void payOrder_AlreadyPaid() {
            ServiceOrder order = createTestOrder(1L, "pending", "paid");
            when(orderRepository.selectById(1L)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.payOrder(1L, "wechat"));

            assertEquals(400, exception.getCode());
        }
    }

    @Nested
    @DisplayName("订单删除测试")
    class OrderDeleteTests {

        @Test
        @DisplayName("删除订单成功")
        void deleteOrder_Success() {
            ServiceOrder order = createTestOrder(1L, "pending", "unpaid");
            when(orderRepository.selectById(1L)).thenReturn(order);
            doNothing().when(orderRepository).deleteById(1L);

            assertDoesNotThrow(() -> orderService.deleteOrder(1L));

            verify(orderRepository).deleteById(1L);
        }

        @Test
        @DisplayName("删除订单-不存在")
        void deleteOrder_NotFound() {
            when(orderRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.deleteOrder(999L));

            assertEquals(404, exception.getCode());
        }
    }

    // ============ 辅助方法 ============

    private ServiceOrder createTestOrder(Long id, String status, String payStatus) {
        ServiceOrder order = new ServiceOrder();
        order.setId(id);
        order.setOrderNo("SO2024041700001");
        order.setUserId(1L);
        order.setStoreId(1L);
        order.setServiceTypeId(1L);
        order.setStaffId(1L);
        order.setPropertyId(1L);
        order.setServiceAddress("测试地址");
        order.setServiceTime(LocalDateTime.now().plusDays(1));
        order.setDuration("2小时");
        order.setTotalAmount(new BigDecimal("200"));
        order.setStatus(status);
        order.setPayStatus(payStatus);
        order.setDeleted(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
