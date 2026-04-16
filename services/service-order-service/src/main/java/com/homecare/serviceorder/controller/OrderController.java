package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.OrderCreateRequest;
import com.homecare.serviceorder.dto.OrderResponse;
import com.homecare.serviceorder.service.ServiceOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 服务订单控制器
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ServiceOrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        OrderResponse response = orderService.createOrder(request, userId);
        return Result.success("下单成功", response);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return Result.success(response);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        OrderResponse response = orderService.cancelOrder(id, reason, userId);
        return Result.success("取消成功", response);
    }

    /**
     * 确认订单
     */
    @PostMapping("/{id}/confirm")
    public Result<OrderResponse> confirmOrder(@PathVariable Long id) {
        OrderResponse response = orderService.confirmOrder(id);
        return Result.success("确认成功", response);
    }

    /**
     * 完成订单
     */
    @PostMapping("/{id}/complete")
    public Result<OrderResponse> completeOrder(@PathVariable Long id) {
        OrderResponse response = orderService.completeOrder(id);
        return Result.success("已完成", response);
    }

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    public Result<OrderResponse> payOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "wechat") String payMethod) {
        OrderResponse response = orderService.payOrder(id, payMethod);
        return Result.success("支付成功", response);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success("删除成功", null);
    }

    /**
     * 分页查询订单
     */
    @GetMapping
    public Result<PageResult<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String payStatus) {
        var result = orderService.listOrders(page, pageSize, userId, storeId, status, payStatus);
        return Result.success(result);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/my")
    public Result<PageResult<OrderResponse>> listMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        var result = orderService.listOrdersByUserId(userId, page, pageSize, status);
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
