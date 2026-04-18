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
     * 创建采购商品（商家提交为待审核；平台管理员可直接上架）
     */
    ProcurementProductResponse create(ProcurementProductRequest request, Long storeId, String operatorRole);

    /**
     * 更新采购商品
     */
    ProcurementProductResponse update(Long id, ProcurementProductRequest request);

    /**
     * 获取采购商品详情（未上架仅商家本人或平台管理员可见）
     */
    ProcurementProductResponse getById(Long id, Long viewerUserId, String viewerRole);

    /**
     * 分页查询采购商品
     */
    PageResult<ProcurementProductResponse> list(int page, int pageSize, String keyword,
            String category, String status, String sort, Long viewerUserId, String viewerRole);

    /**
     * 平台管理员通过采购商品上架审核
     */
    ProcurementProductResponse approveListing(Long id, String operatorRole);

    /**
     * 平台管理员驳回采购商品上架
     */
    ProcurementProductResponse rejectListing(Long id, String operatorRole);

    /**
     * 删除采购商品
     */
    void delete(Long id);

    /**
     * 更新库存
     */
    ProcurementProductResponse updateStock(Long id, int quantity);
}
