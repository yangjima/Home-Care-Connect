package com.homecare.property.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.property.entity.Distributor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分销员 Mapper
 */
@Mapper
public interface DistributorRepository extends BaseMapper<Distributor> {
}
