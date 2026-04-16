package com.homecare.asset.service;

import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.common.PageResult;
import java.util.List;

/**
 * 采购商品服务接口
 */
public interface ProcurementProductService {

    /**
     * 创建采购商品
     */
    ProcurementProductResponse create(ProcurementProductRequest request, Long storeId);

    /**
     * 更新采购商品
     */
    ProcurementProductResponse update(Long id, ProcurementProductRequest request);

    /**
     * 获取采购商品详情
     */
    ProcurementProductResponse getById(Long id);

    /**
     * 分页查询采购商品
     */
    PageResult<ProcurementProductResponse> list(int page, int pageSize, String keyword,
            String category, String status);

    /**
     * 删除采购商品
     */
    void delete(Long id);

    /**
     * 更新库存
     */
    ProcurementProductResponse updateStock(Long id, int quantity);
}
