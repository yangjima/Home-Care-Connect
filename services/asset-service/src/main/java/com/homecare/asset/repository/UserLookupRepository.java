package com.homecare.asset.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 只读查询用户信息（与 user-service 同库时的轻量展示）
 */
@Mapper
public interface UserLookupRepository {

    @Select("SELECT COALESCE(NULLIF(TRIM(real_name), ''), username) FROM sys_user WHERE id = #{id} LIMIT 1")
    String findSellerLabelById(@Param("id") Long id);
}
