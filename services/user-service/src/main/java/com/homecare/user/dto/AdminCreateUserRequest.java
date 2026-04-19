package com.homecare.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台创建用户（密码为前端 RSA 密文，与登录一致）
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * RSA-OAEP 密文（前缀 rsa_oaep_sha256:）
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String realName;
    private String phone;
    private String email;

    /**
     * 服务人员、店长等角色所属门店；超级管理员创建时必须指定（店长账号从本接口创建时会同步写回 store.manager_id）。
     */
    private Long storeId;
}
