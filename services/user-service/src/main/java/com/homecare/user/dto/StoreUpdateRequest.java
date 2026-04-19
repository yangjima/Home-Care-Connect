package com.homecare.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoreUpdateRequest {

    @NotBlank(message = "门店名称不能为空")
    private String name;

    @NotBlank(message = "门店地址不能为空")
    private String address;

    private String phone;
}
