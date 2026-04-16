package com.homecare.serviceorder.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务订单响应
 */
@Data
public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private Long serviceTypeId;
    private String serviceTypeName;
    private Long staffId;
    private String staffName;
    private Long propertyId;
    private String propertyTitle;
    private String serviceAddress;
    private LocalDateTime serviceTime;
    private String duration;
    private BigDecimal totalAmount;
    private String status;
    private String payStatus;
    private String payMethod;
    private LocalDateTime payTime;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
}
