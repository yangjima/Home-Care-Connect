package com.homecare.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homecare.user.entity.Store;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoreRepository extends BaseMapper<Store> {
}
