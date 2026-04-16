package com.homecare.serviceorder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.serviceorder.entity.ServiceOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务订单 Mapper
 */
@Mapper
public interface ServiceOrderRepository extends BaseMapper<ServiceOrder> {
}
