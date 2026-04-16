package com.homecare.serviceorder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.serviceorder.entity.ServiceStaff;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务人员 Mapper
 */
@Mapper
public interface ServiceStaffRepository extends BaseMapper<ServiceStaff> {
}
