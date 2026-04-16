package com.homecare.serviceorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务订单创建请求
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "服务类型ID不能为空")
    private Long serviceTypeId;

    private Long staffId;

    private Long propertyId;

    private String serviceAddress;

    @NotNull(message = "服务时间不能为空")
    private LocalDateTime serviceTime;

    private String duration;

    private String remark;
}
