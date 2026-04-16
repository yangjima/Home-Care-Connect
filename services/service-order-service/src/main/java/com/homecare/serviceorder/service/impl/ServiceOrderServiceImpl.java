package com.homecare.serviceorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.serviceorder.common.BusinessException;
import com.homecare.serviceorder.common.PageResult;
import com.homecare.serviceorder.dto.OrderCreateRequest;
import com.homecare.serviceorder.dto.OrderResponse;
import com.homecare.serviceorder.entity.ServiceOrder;
import com.homecare.serviceorder.entity.ServiceType;
import com.homecare.serviceorder.entity.ServiceStaff;
import com.homecare.serviceorder.repository.ServiceOrderRepository;
import com.homecare.serviceorder.repository.ServiceTypeRepository;
import com.homecare.serviceorder.repository.ServiceStaffRepository;
import com.homecare.serviceorder.service.ServiceOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务订单实现
 */
@Service
@RequiredArgsConstructor
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceStaffRepository staffRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Long userId) {
        ServiceType serviceType = serviceTypeRepository.selectById(request.getServiceTypeId());
        if (serviceType == null) {
            throw new BusinessException(404, "服务类型不存在");
        }

        Long storeId = 1L;
        ServiceStaff staff = null;
        if (request.getStaffId() != null) {
            staff = staffRepository.selectById(request.getStaffId());
            if (staff != null) {
                storeId = staff.getStoreId();
            }
        }

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setServiceTypeId(request.getServiceTypeId());
        order.setStaffId(request.getStaffId());
        order.setPropertyId(request.getPropertyId());
        order.setServiceAddress(request.getServiceAddress());
        order.setServiceTime(request.getServiceTime());
        order.setDuration(request.getDuration());
        order.setTotalAmount(serviceType.getPrice());
        order.setStatus("pending");
        order.setPayStatus("unpaid");
        order.setRemark(request.getRemark());

        orderRepository.insert(order);
        return toResponse(order);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return toResponse(order);
    }

    @Override
    public PageResult<OrderResponse> listOrders(int page, int pageSize, Long userId, Long storeId,
            String status, String payStatus) {
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(ServiceOrder::getUserId, userId);
        if (storeId != null) wrapper.eq(ServiceOrder::getStoreId, storeId);
        if (status != null) wrapper.eq(ServiceOrder::getStatus, status);
        if (payStatus != null) wrapper.eq(ServiceOrder::getPayStatus, payStatus);
        wrapper.orderByDesc(ServiceOrder::getCreateTime);

        Page<ServiceOrder> pageResult = orderRepository.selectPage(
                new Page<>(page, pageSize), wrapper);

        return new PageResult<>(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String reason, Long userId) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"pending".equals(order.getStatus()) && !"confirmed".equals(order.getStatus())) {
            throw new BusinessException(400, "当前状态无法取消订单");
        }

        order.setStatus("cancelled");
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        orderRepository.updateById(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long id) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(400, "当前状态无法确认订单");
        }

        order.setStatus("confirmed");
        orderRepository.updateById(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long id) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"confirmed".equals(order.getStatus())) {
            throw new BusinessException(400, "当前状态无法完成订单");
        }

        order.setStatus("completed");
        orderRepository.updateById(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse payOrder(Long id, String payMethod) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(400, "订单已支付");
        }

        order.setPayStatus("paid");
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        orderRepository.updateById(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        ServiceOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        orderRepository.deleteById(id);
    }

    @Override
    public PageResult<OrderResponse> listOrdersByUserId(Long userId, int page, int pageSize, String status) {
        return listOrders(page, pageSize, userId, null, status, null);
    }

    @Override
    public PageResult<OrderResponse> listOrdersByStoreId(Long storeId, int page, int pageSize, String status) {
        return listOrders(page, pageSize, null, storeId, status, null);
    }

    private OrderResponse toResponse(ServiceOrder order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setStoreId(order.getStoreId());
        response.setServiceTypeId(order.getServiceTypeId());
        response.setStaffId(order.getStaffId());
        response.setPropertyId(order.getPropertyId());
        response.setServiceAddress(order.getServiceAddress());
        response.setServiceTime(order.getServiceTime());
        response.setDuration(order.getDuration());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPayStatus(order.getPayStatus());
        response.setPayMethod(order.getPayMethod());
        response.setPayTime(order.getPayTime());
        response.setRemark(order.getRemark());
        response.setCancelReason(order.getCancelReason());
        response.setCancelTime(order.getCancelTime());
        response.setCreateTime(order.getCreateTime());

        if (order.getServiceTypeId() != null) {
            ServiceType serviceType = serviceTypeRepository.selectById(order.getServiceTypeId());
            if (serviceType != null) {
                response.setServiceTypeName(serviceType.getName());
            }
        }

        if (order.getStaffId() != null) {
            ServiceStaff staff = staffRepository.selectById(order.getStaffId());
            if (staff != null) {
                response.setStaffName(staff.getName());
            }
        }

        return response;
    }

    private String generateOrderNo() {
        return "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
    }
}
