package com.homecare.serviceorder.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价响应
 */
@Data
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private String userName;
    private Long staffId;
    private String staffName;
    private Long storeId;
    private Integer rating;
    private String content;
    private String images;
    private Integer isAnonymous;
    private LocalDateTime createTime;
}
