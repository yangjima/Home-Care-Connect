package com.homecare.property.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.property.entity.Property;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房产 Mapper
 */
@Mapper
public interface PropertyRepository extends BaseMapper<Property> {
}
