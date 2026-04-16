package com.homecare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PageResult;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        );
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, User user) {
        User existingUser = userRepository.selectById(id);
        if (existingUser == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 只允许更新部分字段
        if (user.getRealName() != null) {
            existingUser.setRealName(user.getRealName());
        }
        if (user.getGender() != null) {
            existingUser.setGender(user.getGender());
        }
        if (user.getPhone() != null) {
            // 检查手机号是否被其他用户占用
            if (isPhoneExistsForOther(user.getPhone(), id)) {
                throw new BusinessException(400, "手机号已被其他用户使用");
            }
            existingUser.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            // 检查邮箱是否被其他用户占用
            if (isEmailExistsForOther(user.getEmail(), id)) {
                throw new BusinessException(400, "邮箱已被其他用户使用");
            }
            existingUser.setEmail(user.getEmail());
        }
        if (user.getAvatar() != null) {
            existingUser.setAvatar(user.getAvatar());
        }

        userRepository.updateById(existingUser);
        log.info("更新用户信息: {}", id);

        return toUserResponse(existingUser);
    }

    @Override
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public PageResult<UserResponse> listUsers(int page, int pageSize, String role, String keyword) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword)
            );
        }

        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userRepository.selectPage(pageParam, wrapper);

        List<UserResponse> records = result.getRecords().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, records);
    }

    @Override
    public void updateUserStatus(Long id, String status) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!"active".equals(status) && !"inactive".equals(status) && !"banned".equals(status)) {
            throw new BusinessException(400, "无效的状态值");
        }

        user.setStatus(status);
        userRepository.updateById(user);

        log.info("更新用户 {} 状态为 {}", user.getUsername(), status);
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    /**
     * 检查手机号是否被其他用户占用
     */
    private boolean isPhoneExistsForOther(String phone, Long excludeId) {
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getId, excludeId)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    /**
     * 检查邮箱是否被其他用户占用
     */
    private boolean isEmailExistsForOther(String email, Long excludeId) {
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .ne(User::getId, excludeId)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    /**
     * 将 User 实体转换为 UserResponse
     */
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        // 不返回密码
        return response;
    }
}
