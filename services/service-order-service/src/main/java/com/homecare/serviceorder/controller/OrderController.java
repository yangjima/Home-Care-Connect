package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.OrderCreateRequest;
import com.homecare.serviceorder.dto.OrderResponse;
import com.homecare.serviceorder.service.ServiceOrderService;
import com.homecare.serviceorder.util.GatewayHeaders;
import com.homecare.serviceorder.util.Roles;
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
        Long userId = requireUserId(httpRequest);
        OrderResponse response = orderService.createOrder(request, userId);
        return Result.success("下单成功", response);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderResponse> getOrderById(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        OrderResponse response = orderService.getOrderById(id);
        if (!Roles.canListAllOrders(role) && (response.getUserId() == null || !response.getUserId().equals(userId))) {
            return Result.forbidden("无权查看该订单");
        }
        return Result.success(response);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<OrderResponse> cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        OrderResponse existing = orderService.getOrderById(id);
        if (!Roles.isPlatformAdmin(role) && (existing.getUserId() == null || !existing.getUserId().equals(userId))) {
            return Result.forbidden("无权取消该订单");
        }
        OrderResponse response = orderService.cancelOrder(id, reason, userId);
        return Result.success("取消成功", response);
    }

    /**
     * 确认订单
     */
    @PostMapping("/{id}/confirm")
    public Result<OrderResponse> confirmOrder(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireStaffRole(httpRequest);
        OrderResponse response = orderService.confirmOrder(id);
        return Result.success("确认成功", response);
    }

    /**
     * 完成订单
     */
    @PostMapping("/{id}/complete")
    public Result<OrderResponse> completeOrder(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireStaffRole(httpRequest);
        OrderResponse response = orderService.completeOrder(id);
        return Result.success("已完成", response);
    }

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    public Result<OrderResponse> payOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "payMethod", defaultValue = "wechat") String payMethod,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        OrderResponse existing = orderService.getOrderById(id);
        if (!Roles.isPlatformAdmin(role) && (existing.getUserId() == null || !existing.getUserId().equals(userId))) {
            return Result.forbidden("无权支付该订单");
        }
        OrderResponse response = orderService.payOrder(id, payMethod);
        return Result.success("支付成功", response);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        requireStaffRole(httpRequest);
        orderService.deleteOrder(id);
        return Result.success("删除成功", null);
    }

    /**
     * 分页查询订单
     */
    @GetMapping
    public Result<PageResult<OrderResponse>> listOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "payStatus", required = false) String payStatus,
            HttpServletRequest httpRequest) {
        Long requestUserId = requireUserId(httpRequest);
        String role = GatewayHeaders.role(httpRequest);
        Long effectiveUserId = userId;
        if (!Roles.canListAllOrders(role)) {
            effectiveUserId = requestUserId;
        }
        var result = orderService.listOrders(page, pageSize, effectiveUserId, storeId, status, payStatus);
        return Result.success(result);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/my")
    public Result<PageResult<OrderResponse>> listMyOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        var result = orderService.listOrdersByUserId(userId, page, pageSize, status);
        return Result.success(result);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = GatewayHeaders.userId(request);
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void requireStaffRole(HttpServletRequest request) {
        String role = GatewayHeaders.role(request);
        if (!Roles.isStaffOperator(role)) {
            throw new BusinessException(403, "仅超级管理员或店长可操作");
        }
    }
}
