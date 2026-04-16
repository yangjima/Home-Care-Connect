package com.homecare.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.entity.SecondhandItem;
import com.homecare.asset.repository.SecondhandItemRepository;
import com.homecare.asset.service.SecondhandItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 二手商品服务实现
 */
@Service
@RequiredArgsConstructor
public class SecondhandItemServiceImpl implements SecondhandItemService {

    private final SecondhandItemRepository itemRepository;

    @Override
    @Transactional
    public SecondhandItemResponse create(SecondhandItemRequest request, Long userId) {
        SecondhandItem item = new SecondhandItem();
        item.setUserId(userId);
        item.setStoreId(request.getStoreId() != null ? request.getStoreId() : 1L);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setCondition(request.getCondition());
        item.setImage(request.getImage());
        item.setImages(request.getImages());
        item.setStatus("pending");
        item.setViewCount(0L);
        item.setExpireTime(request.getExpireTime() != null ? request.getExpireTime() :
                LocalDateTime.now().plusDays(30));

        itemRepository.insert(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public SecondhandItemResponse update(Long id, SecondhandItemRequest request, Long userId) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!userId.equals(item.getUserId())) {
            throw new BusinessException(403, "无权限修改");
        }

        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCondition() != null) item.setCondition(request.getCondition());
        if (request.getImage() != null) item.setImage(request.getImage());
        if (request.getImages() != null) item.setImages(request.getImages());

        itemRepository.updateById(item);
        return toResponse(item);
    }

    @Override
    public SecondhandItemResponse getById(Long id) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return toResponse(item);
    }

    @Override
    public PageResult<SecondhandItemResponse> list(int page, int pageSize, String keyword,
            String category, String condition, String status) {
        LambdaQueryWrapper<SecondhandItem> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null) {
            wrapper.like(SecondhandItem::getTitle, keyword)
                   .or()
                   .like(SecondhandItem::getDescription, keyword);
        }
        if (category != null) wrapper.eq(SecondhandItem::getCategory, category);
        if (condition != null) wrapper.eq(SecondhandItem::getCondition, condition);
        if (status != null) wrapper.eq(SecondhandItem::getStatus, status);
        wrapper.orderByDesc(SecondhandItem::getCreateTime);

        Page<SecondhandItem> pageResult = itemRepository.selectPage(
                new Page<>(page, pageSize), wrapper);

        return new PageResult<>(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!userId.equals(item.getUserId())) {
            throw new BusinessException(403, "无权限删除");
        }
        itemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            return;
        }
        item.setViewCount(item.getViewCount() == null ? 1L : item.getViewCount() + 1);
        itemRepository.updateById(item);
    }

    @Override
    public PageResult<SecondhandItemResponse> listByUserId(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<SecondhandItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecondhandItem::getUserId, userId)
               .orderByDesc(SecondhandItem::getCreateTime);

        Page<SecondhandItem> pageResult = itemRepository.selectPage(
                new Page<>(page, pageSize), wrapper);

        return new PageResult<>(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    private SecondhandItemResponse toResponse(SecondhandItem item) {
        SecondhandItemResponse response = new SecondhandItemResponse();
        response.setId(item.getId());
        response.setUserId(item.getUserId());
        response.setStoreId(item.getStoreId());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setCategory(item.getCategory());
        response.setPrice(item.getPrice());
        response.setCondition(item.getCondition());
        response.setImage(item.getImage());
        response.setImages(item.getImages());
        response.setStatus(item.getStatus());
        response.setViewCount(item.getViewCount());
        response.setExpireTime(item.getExpireTime());
        response.setCreateTime(item.getCreateTime());
        return response;
    }
}
