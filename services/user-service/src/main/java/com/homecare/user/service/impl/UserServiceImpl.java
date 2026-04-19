package com.homecare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PageResult;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.AdminCreateUserRequest;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.dto.UserStatsResponse;
import com.homecare.user.entity.Store;
import com.homecare.user.entity.User;
import com.homecare.user.repository.StoreRepository;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.security.RsaPasswordDecryptor;
import com.homecare.user.service.UserService;
import com.homecare.user.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private static final Set<String> ASSIGNABLE_BY_ADMIN = Set.of(
            "admin", "store_manager", "supplier", "service_staff", "distributor", "user");
    private static final Set<String> ASSIGNABLE_BY_STORE_MANAGER = Set.of(
            "store_manager", "service_staff", "distributor", "user");

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RsaPasswordDecryptor rsaPasswordDecryptor;

    /**
     * 「在线」统计：最近 N 分钟内有登录记录的用户数
     */
    @Value("${app.user-stats.online-within-minutes:15}")
    private int onlineWithinMinutes;

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

        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            throw new BusinessException(400, "用户名不允许修改");
        }
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            throw new BusinessException(400, "邮箱不允许修改");
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
    public PageResult<UserResponse> listUsers(int page, int pageSize, String role, String keyword,
            String operatorRole, Long operatorStoreId) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (Roles.STORE_MANAGER.equals(operatorRole)) {
            if (operatorStoreId == null) {
                throw new BusinessException(403, "店长账号未绑定门店，无法查看用户列表");
            }
            wrapper.eq(User::getStoreId, operatorStoreId);
        }
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
    public UserStatsResponse getUserStats(String operatorRole, Long operatorStoreId) {
        if (Roles.STORE_MANAGER.equals(operatorRole)) {
            if (operatorStoreId == null) {
                return new UserStatsResponse(0, 0, 0, 0);
            }
            LambdaQueryWrapper<User> base = new LambdaQueryWrapper<User>().eq(User::getStoreId, operatorStoreId);
            long total = userRepository.selectCount(base);
            long serviceStaff = userRepository.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getStoreId, operatorStoreId)
                            .eq(User::getRole, "service_staff"));
            long distributor = userRepository.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getStoreId, operatorStoreId)
                            .eq(User::getRole, "distributor"));
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(onlineWithinMinutes);
            long online = userRepository.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getStoreId, operatorStoreId)
                            .isNotNull(User::getLastLoginAt)
                            .ge(User::getLastLoginAt, threshold));
            return new UserStatsResponse(total, serviceStaff, distributor, online);
        }
        long total = userRepository.selectCount(null);
        long serviceStaff = userRepository.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "service_staff"));
        long distributor = userRepository.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "distributor"));
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(onlineWithinMinutes);
        long online = userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getLastLoginAt)
                        .ge(User::getLastLoginAt, threshold));
        return new UserStatsResponse(total, serviceStaff, distributor, online);
    }

    @Override
    @Transactional
    public UserResponse createUserByAdmin(AdminCreateUserRequest request, String operatorRole, Long operatorStoreId) {
        String normalizedRole = request.getRole() == null ? "" : request.getRole().trim();
        if (!KNOWN_ROLES.contains(normalizedRole)) {
            throw new BusinessException(400, "无效角色: " + normalizedRole);
        }
        assertAssignableRole(operatorRole, normalizedRole);

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (isUsernameExists(username)) {
            throw new BusinessException(400, "用户名已存在");
        }
        String phone = blankToNull(request.getPhone());
        String email = blankToNull(request.getEmail());
        if (phone != null && isPhoneExists(phone)) {
            throw new BusinessException(400, "手机号已被注册");
        }
        if (email != null && isEmailExists(email)) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        String rawPassword = rsaPasswordDecryptor.decryptRequired(request.getPassword());
        if (rawPassword.length() < 6 || rawPassword.length() > 20) {
            throw new BusinessException(400, "密码长度必须在6-20个字符之间");
        }

        Long resolvedStoreId = resolveTargetStoreId(operatorRole, operatorStoreId, request.getStoreId());
        boolean needsStore = "store_manager".equals(normalizedRole) || "service_staff".equals(normalizedRole);
        if (needsStore && resolvedStoreId == null) {
            throw new BusinessException(400, "请选择所属门店");
        }
        if (resolvedStoreId != null && storeRepository.selectById(resolvedStoreId) == null) {
            throw new BusinessException(400, "门店不存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(normalizedRole);
        user.setStatus("active");
        if (resolvedStoreId != null) {
            user.setStoreId(resolvedStoreId);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (request.getRealName() != null && !request.getRealName().isBlank()) {
            user.setRealName(request.getRealName().trim());
        }

        userRepository.insert(user);
        if ("store_manager".equals(normalizedRole) && user.getStoreId() != null) {
            syncStoreManager(user.getStoreId(), user.getId());
        }

        log.info("管理后台创建用户: {} 角色={} 门店={} 操作者角色={}", username, normalizedRole, user.getStoreId(), operatorRole);
        return toUserResponse(user);
    }

    private Long resolveTargetStoreId(String operatorRole, Long operatorStoreId, Long requestedStoreId) {
        if (Roles.STORE_MANAGER.equals(operatorRole)) {
            return operatorStoreId;
        }
        return requestedStoreId;
    }

    /**
     * 新任店长绑定门店：门店表 manager_id 指向该用户，并解除同店其他店长账号的门店归属。
     */
    private void syncStoreManager(Long storeId, Long newManagerUserId) {
        Store store = storeRepository.selectById(storeId);
        if (store == null) {
            return;
        }
        userRepository.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getStoreId, storeId)
                .eq(User::getRole, Roles.STORE_MANAGER)
                .ne(User::getId, newManagerUserId)
                .set(User::getStoreId, null));
        store.setManagerId(newManagerUserId);
        storeRepository.updateById(store);
    }

    @Override
    @Transactional
    public UserResponse updateUserStore(Long userId, Long storeId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (storeId != null && storeRepository.selectById(storeId) == null) {
            throw new BusinessException(400, "门店不存在");
        }
        user.setStoreId(storeId);
        userRepository.updateById(user);
        if ("store_manager".equals(user.getRole()) && storeId != null) {
            syncStoreManager(storeId, user.getId());
        }
        return toUserResponse(user);
    }

    private void assertAssignableRole(String operatorRole, String targetRole) {
        Set<String> allowed;
        if (Roles.ADMIN.equals(operatorRole)) {
            allowed = ASSIGNABLE_BY_ADMIN;
        } else if (Roles.STORE_MANAGER.equals(operatorRole)) {
            allowed = ASSIGNABLE_BY_STORE_MANAGER;
        } else {
            throw new BusinessException(403, "无权创建用户");
        }
        if (!allowed.contains(targetRole)) {
            throw new BusinessException(403, "无权创建该角色的用户");
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
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

    private static final Set<String> KNOWN_ROLES = Set.of(
            "admin", "store_manager", "supplier", "tenant", "user",
            "distributor", "service_staff");

    @Override
    public UserResponse updateUserRole(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new BusinessException(400, "角色不能为空");
        }
        String normalized = role.trim();
        if (!KNOWN_ROLES.contains(normalized)) {
            throw new BusinessException(400, "无效角色: " + normalized);
        }
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setRole(normalized);
        userRepository.updateById(user);
        log.info("更新用户 {} 角色为 {}", user.getUsername(), normalized);
        return toUserResponse(user);
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
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
        ) > 0;
    }

    private static final Set<String> DELETABLE_BY_STORE_MANAGER = Set.of(
            "service_staff", "distributor", "tenant", "user");

    @Override
    public void deleteUser(Long id, String operatorRole, Long operatorId, Long operatorStoreId) {
        if (id == null || operatorId == null) {
            throw new BusinessException(400, "参数无效");
        }
        if (id.equals(operatorId)) {
            throw new BusinessException(400, "不能删除当前登录账号");
        }
        User target = userRepository.selectById(id);
        if (target == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String targetRole = target.getRole() == null ? "" : target.getRole();
        if (Roles.ADMIN.equals(operatorRole)) {
            if (Roles.ADMIN.equals(targetRole)) {
                throw new BusinessException(403, "不能删除其他管理员账号");
            }
        } else if (Roles.STORE_MANAGER.equals(operatorRole)) {
            if (!DELETABLE_BY_STORE_MANAGER.contains(targetRole)) {
                throw new BusinessException(403, "无权删除该用户");
            }
            if (operatorStoreId == null || target.getStoreId() == null
                    || !operatorStoreId.equals(target.getStoreId())) {
                throw new BusinessException(403, "仅能删除本门店用户");
            }
        } else {
            throw new BusinessException(403, "无权删除用户");
        }
        userRepository.deleteById(id);
        log.info("管理后台删除用户: id={} username={} 操作者角色={}", id, target.getUsername(), operatorRole);
    }

    /**
     * 检查手机号是否被其他用户占用
     */
    private boolean isPhoneExistsForOther(String phone, Long excludeId) {
        return userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getId, excludeId)
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
