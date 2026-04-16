package com.homecare.serviceorder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.serviceorder.entity.ServiceType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务类型 Mapper
 */
@Mapper
public interface ServiceTypeRepository extends BaseMapper<ServiceType> {
}
