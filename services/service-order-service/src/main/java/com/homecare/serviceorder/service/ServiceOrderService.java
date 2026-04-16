package com.homecare.serviceorder.service;

import com.homecare.serviceorder.dto.OrderCreateRequest;
import com.homecare.serviceorder.dto.OrderResponse;
import com.homecare.serviceorder.common.PageResult;
import java.util.List;

/**
 * 服务订单接口
 */
public interface ServiceOrderService {

    /**
     * 创建订单
     */
    OrderResponse createOrder(OrderCreateRequest request, Long userId);

    /**
     * 获取订单详情
     */
    OrderResponse getOrderById(Long id);

    /**
     * 分页查询订单
     */
    PageResult<OrderResponse> listOrders(int page, int pageSize, Long userId, Long storeId,
            String status, String payStatus);

    /**
     * 取消订单
     */
    OrderResponse cancelOrder(Long id, String reason, Long userId);

    /**
     * 确认订单
     */
    OrderResponse confirmOrder(Long id);

    /**
     * 完成订单
     */
    OrderResponse completeOrder(Long id);

    /**
     * 支付订单
     */
    OrderResponse payOrder(Long id, String payMethod);

    /**
     * 删除订单
     */
    void deleteOrder(Long id);

    /**
     * 获取用户订单列表
     */
    PageResult<OrderResponse> listOrdersByUserId(Long userId, int page, int pageSize, String status);

    /**
     * 获取门店订单列表
     */
    PageResult<OrderResponse> listOrdersByStoreId(Long storeId, int page, int pageSize, String status);
}
