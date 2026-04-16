package com.homecare.asset.service;

import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.common.PageResult;

/**
 * 二手商品服务接口
 */
public interface SecondhandItemService {

    /**
     * 发布二手商品
     */
    SecondhandItemResponse create(SecondhandItemRequest request, Long userId);

    /**
     * 更新二手商品
     */
    SecondhandItemResponse update(Long id, SecondhandItemRequest request, Long userId);

    /**
     * 获取二手商品详情
     */
    SecondhandItemResponse getById(Long id);

    /**
     * 分页查询二手商品
     */
    PageResult<SecondhandItemResponse> list(int page, int pageSize, String keyword,
            String category, String condition, String status);

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
}
