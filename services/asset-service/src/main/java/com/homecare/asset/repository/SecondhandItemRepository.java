package com.homecare.asset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.asset.entity.SecondhandItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 二手商品 Mapper
 */
@Mapper
public interface SecondhandItemRepository extends BaseMapper<SecondhandItem> {
}
