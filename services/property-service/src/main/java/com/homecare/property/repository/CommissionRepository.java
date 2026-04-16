package com.homecare.property.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.property.entity.Commission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 佣金记录 Mapper
 */
@Mapper
public interface CommissionRepository extends BaseMapper<Commission> {
}
