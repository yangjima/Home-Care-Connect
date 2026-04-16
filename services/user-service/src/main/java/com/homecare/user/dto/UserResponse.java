package com.homecare.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String realName;
    private String gender;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private Long storeId;
    private String storeName;
    private String status;
    private LocalDateTime createTime;
}
