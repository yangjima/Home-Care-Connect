package com.homecare.serviceorder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.serviceorder.entity.ServiceStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务人员 Mapper
 */
@Mapper
public interface ServiceStaffRepository extends BaseMapper<ServiceStaff> {

    @Select("SELECT s.id, s.user_id AS userId, s.store_id AS storeId, s.status, s.rating AS starRating, "
            + "s.total_orders AS orderCount, s.skills, u.username AS name, u.avatar AS avatar "
            + "FROM service_staff s INNER JOIN sys_user u ON u.id = s.user_id "
            + "WHERE s.status IN ('available','busy') ORDER BY s.rating DESC, s.total_orders DESC LIMIT 24")
    List<ServiceStaff> selectPublicWithUser();
}
