package com.homecare.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
}
