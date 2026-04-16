package com.homecare.user.controller;

import com.homecare.user.common.PageResult;
import com.homecare.user.common.Result;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;
import com.homecare.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id, @RequestBody User user) {
        UserResponse response = userService.updateUser(id, user);
        return Result.success("更新成功", response);
    }

    /**
     * 更新密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(
            @PathVariable Long id,
            @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(id, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码更新成功", null);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<PageResult<UserResponse>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        PageResult<UserResponse> result = userService.listUsers(page, pageSize, role, keyword);
        return Result.success(result);
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        userService.updateUserStatus(id, request.getStatus());
        return Result.success("状态更新成功", null);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check/username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.isUsernameExists(username);
        return Result.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check/phone")
    public Result<Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = userService.isPhoneExists(phone);
        return Result.success(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check/email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailExists(email);
        return Result.success(exists);
    }

    // --- 内部请求类 ---

    @lombok.Data
    public static class PasswordUpdateRequest {
        private String oldPassword;
        private String newPassword;
    }

    @lombok.Data
    public static class StatusUpdateRequest {
        private String status;
    }
}
