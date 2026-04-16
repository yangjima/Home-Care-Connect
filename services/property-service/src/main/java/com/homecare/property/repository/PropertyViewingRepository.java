package com.homecare.property.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.property.entity.PropertyViewing;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约看房 Mapper
 */
@Mapper
public interface PropertyViewingRepository extends BaseMapper<PropertyViewing> {
}
