package com.homecare.serviceorder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.serviceorder.entity.ServiceReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价 Mapper
 */
@Mapper
public interface ServiceReviewRepository extends BaseMapper<ServiceReview> {
}
