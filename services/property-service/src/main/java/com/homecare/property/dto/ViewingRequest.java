package com.homecare.property.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约看房请求
 */
@Data
public class ViewingRequest {

    private Long propertyId;
    private LocalDateTime viewingTime;
    private String remark;
}
