package com.homecare.user.controller;

import com.homecare.user.common.PageResult;
import com.homecare.user.common.Result;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;
import com.homecare.user.service.UserService;
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

    @Value("${minio.bucket.avatar:homecare-avatar}")
    private String avatarBucket;

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
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "keyword", required = false) String keyword) {
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

        Long userId = getUserId(request);
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

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return 1L;
        }
        return (Long) userId;
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
