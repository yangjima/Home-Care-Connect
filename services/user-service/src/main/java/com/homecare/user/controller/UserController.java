package com.homecare.user.controller;

import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PageResult;
import com.homecare.user.common.Result;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;
import com.homecare.user.security.RsaPasswordDecryptor;
import com.homecare.user.service.UserService;
import com.homecare.user.util.GatewayHeaders;
import com.homecare.user.util.Roles;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MinioClient minioClient;
    private final RsaPasswordDecryptor rsaPasswordDecryptor;

    @Value("${minio.bucket.avatar:homecare-avatar}")
    private String avatarBucket;

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<UserResponse> getUserById(@PathVariable("id") Long id, HttpServletRequest request) {
        Long operatorId = requireUserId(request);
        String role = GatewayHeaders.role(request);
        if (!id.equals(operatorId) && !Roles.isPlatformAdmin(role)) {
            return Result.error(403, "无权查看该用户");
        }
        UserResponse response = userService.getUserById(id);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable("id") Long id, @RequestBody User user, HttpServletRequest request) {
        Long operatorId = requireUserId(request);
        String role = GatewayHeaders.role(request);
        if (!id.equals(operatorId) && !Roles.isPlatformAdmin(role)) {
            return Result.error(403, "无权修改该用户");
        }
        UserResponse response = userService.updateUser(id, user);
        return Result.success("更新成功", response);
    }

    /**
     * 更新密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(
            @PathVariable("id") Long id,
            @RequestBody PasswordUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = requireUserId(httpRequest);
        if (!id.equals(operatorId)) {
            return Result.error(403, "只能修改自己的密码");
        }
        String oldPassword = rsaPasswordDecryptor.decryptRequired(request.getOldPassword());
        String newPassword = rsaPasswordDecryptor.decryptRequired(request.getNewPassword());
        userService.updatePassword(id, oldPassword, newPassword);
        return Result.success("密码更新成功", null);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<PageResult<UserResponse>> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        requirePlatformAdmin(request);
        PageResult<UserResponse> result = userService.listUsers(page, pageSize, role, keyword);
        return Result.success(result);
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @PathVariable("id") Long id,
            @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        userService.updateUserStatus(id, request.getStatus());
        return Result.success("状态更新成功", null);
    }

    /**
     * 更新用户角色（超级管理员 / 店长）
     */
    @PatchMapping("/{id}/role")
    public Result<UserResponse> updateUserRole(
            @PathVariable("id") Long id,
            @RequestBody RoleUpdateRequest request,
            HttpServletRequest httpRequest) {
        requirePlatformAdmin(httpRequest);
        UserResponse updated = userService.updateUserRole(id, request.getRole());
        return Result.success("角色更新成功", updated);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check/username")
    public Result<Boolean> checkUsername(@RequestParam("username") String username) {
        boolean exists = userService.isUsernameExists(username);
        return Result.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check/phone")
    public Result<Boolean> checkPhone(@RequestParam("phone") String phone) {
        boolean exists = userService.isPhoneExists(phone);
        return Result.success(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check/email")
    public Result<Boolean> checkEmail(@RequestParam("email") String email) {
        boolean exists = userService.isEmailExists(email);
        return Result.success(exists);
    }

    /**
     * 上传头像
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的头像文件");
        }
        if (!StringUtils.hasText(file.getContentType()) || !file.getContentType().startsWith("image/")) {
            return Result.error(400, "只支持图片文件");
        }

        Long userId = requireUserId(request);
        String ext = getFileExtension(file.getOriginalFilename());
        String objectName = "user/" + userId + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }

        String avatarUrl = "/minio/" + avatarBucket + "/" + objectName;

        User patch = new User();
        patch.setAvatar(avatarUrl);
        UserResponse updated = userService.updateUser(userId, patch);

        Map<String, Object> data = new HashMap<>();
        data.put("avatar", avatarUrl);
        data.put("user", updated);
        return Result.success("上传成功", data);
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

    @lombok.Data
    public static class RoleUpdateRequest {
        private String role;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = GatewayHeaders.userId(request);
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void requirePlatformAdmin(HttpServletRequest request) {
        String role = GatewayHeaders.role(request);
        if (!Roles.isPlatformAdmin(role)) {
            throw new BusinessException(403, "仅超级管理员或店长可访问");
        }
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".jpg";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return ".jpg";
        }
        return filename.substring(index);
    }
}
