package com.homecare.user.service;

import com.homecare.user.common.PageResult;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户详情
     */
    UserResponse getUserById(Long id);

    /**
     * 获取用户详情（通过用户名）
     */
    UserResponse getUserByUsername(String username);

    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long id, User user);

    /**
     * 更新密码
     */
    void updatePassword(Long id, String oldPassword, String newPassword);

    /**
     * 分页查询用户列表
     */
    PageResult<UserResponse> listUsers(int page, int pageSize, String role, String keyword);

    /**
     * 禁用/启用用户
     */
    void updateUserStatus(Long id, String status);

    /**
     * 检查用户名是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查手机号是否存在
     */
    boolean isPhoneExists(String phone);

    /**
     * 检查邮箱是否存在
     */
    boolean isEmailExists(String email);
}
