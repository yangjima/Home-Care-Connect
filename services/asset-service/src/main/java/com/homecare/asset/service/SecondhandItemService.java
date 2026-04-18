package com.homecare.asset.service;

import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.dto.SecondhandMarketStatsResponse;
import com.homecare.asset.common.PageResult;

/**
 * 二手商品服务接口
 */
public interface SecondhandItemService {

    /**
     * 发布二手商品
     */
    SecondhandItemResponse create(SecondhandItemRequest request, Long userId, String operatorRole);

    /**
     * 更新二手商品
     */
    SecondhandItemResponse update(Long id, SecondhandItemRequest request, Long userId);

    /**
     * 获取二手商品详情（待审核仅发布者或平台管理员可见）
     */
    SecondhandItemResponse getById(Long id, Long viewerUserId, String viewerRole);

    /**
     * 分页查询二手商品
     */
    PageResult<SecondhandItemResponse> list(int page, int pageSize, String keyword,
            String category, String condition, String status, Long viewerUserId, String viewerRole);

    /**
     * 删除二手商品
     */
    void delete(Long id, Long userId);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);

    /**
     * 获取用户发布的商品
     */
    PageResult<SecondhandItemResponse> listByUserId(Long userId, int page, int pageSize);

    SecondhandMarketStatsResponse getMarketStats();

    /**
     * 平台管理员通过跳蚤商品上架审核
     */
    SecondhandItemResponse approveListing(Long id, String operatorRole);

    /**
     * 平台管理员驳回跳蚤商品上架
     */
    SecondhandItemResponse rejectListing(Long id, String operatorRole);

    /**
     * 发布者将已驳回/下架的商品重新提交审核
     */
    SecondhandItemResponse submitForListing(Long id, Long userId);
}
