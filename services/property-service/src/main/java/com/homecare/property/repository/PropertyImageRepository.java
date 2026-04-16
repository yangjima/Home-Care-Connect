package com.homecare.property.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.property.entity.PropertyImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房产图片 Mapper
 */
@Mapper
public interface PropertyImageRepository extends BaseMapper<PropertyImage> {
}
