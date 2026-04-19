package com.homecare.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private Long managerId;
    private LocalDateTime createTime;
}
