package com.homecare.property.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约看房响应
 */
@Data
public class ViewingResponse {

    private Long id;
    private Long propertyId;
    private String propertyTitle;
    private Long userId;
    private String userName;
    private Long distributorId;
    private String distributorName;
    private LocalDateTime viewingTime;
    private String status;
    private String userRemark;
    private String adminRemark;
    private LocalDateTime createTime;
}
