package com.homecare.asset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.asset.entity.ProcurementProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购商品 Mapper
 */
@Mapper
public interface ProcurementProductRepository extends BaseMapper<ProcurementProduct> {
}
