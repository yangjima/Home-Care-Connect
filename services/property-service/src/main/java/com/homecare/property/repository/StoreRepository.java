package com.homecare.property.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 门店表只读查询（房源需关联有效 {@code store.id}）。
 */
@Mapper
public interface StoreRepository {

    @Select("SELECT id FROM store ORDER BY id ASC LIMIT 1")
    Long selectFirstStoreId();

    @Select("SELECT COUNT(1) FROM store WHERE id = #{id}")
    int countById(Long id);
}
