package com.homecare.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.dto.SecondhandMarketStatsResponse;
import com.homecare.asset.entity.SecondhandItem;
import com.homecare.asset.repository.SecondhandItemRepository;
import com.homecare.asset.repository.UserLookupRepository;
import com.homecare.asset.service.SecondhandItemService;
import com.homecare.asset.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 二手商品服务实现
 */
@Service
@RequiredArgsConstructor
public class SecondhandItemServiceImpl implements SecondhandItemService {

    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    private final SecondhandItemRepository itemRepository;
    private final UserLookupRepository userLookupRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SecondhandItemResponse create(SecondhandItemRequest request, Long userId, String operatorRole) {
        SecondhandItem item = new SecondhandItem();
        item.setUserId(userId);
        item.setStoreId(request.getStoreId() != null ? request.getStoreId() : 1L);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setOriginalPrice(request.getOriginalPrice());
        item.setCondition(request.getCondition());
        item.setContact(request.getContact());
        item.setLocation(request.getLocation());
        item.setImage(request.getImage());
        item.setImages(request.getImages());
        item.setStatus(Roles.isPlatformAdmin(operatorRole) ? "1" : "2");
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
        if (request.getOriginalPrice() != null) item.setOriginalPrice(request.getOriginalPrice());
        if (request.getCondition() != null) item.setCondition(request.getCondition());
        if (request.getContact() != null) item.setContact(request.getContact());
        if (request.getLocation() != null) item.setLocation(request.getLocation());
        if (request.getImage() != null) item.setImage(request.getImage());
        if (request.getImages() != null) item.setImages(request.getImages());

        itemRepository.updateById(item);
        return toResponse(item);
    }

    @Override
    public SecondhandItemResponse getById(Long id, Long viewerUserId, String viewerRole) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        assertSecondhandReadable(item, viewerUserId, viewerRole);
        incrementViewCount(id);
        return toResponse(item);
    }

    @Override
    public PageResult<SecondhandItemResponse> list(int page, int pageSize, String keyword,
            String category, String condition, String status, Long viewerUserId, String viewerRole) {
        LambdaQueryWrapper<SecondhandItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SecondhandItem::getTitle, keyword)
                    .or()
                    .like(SecondhandItem::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(SecondhandItem::getCategory, category);
        }
        if (StringUtils.hasText(condition)) {
            wrapper.eq(SecondhandItem::getCondition, condition);
        }
        if (StringUtils.hasText(status) && Roles.isPlatformAdmin(viewerRole)) {
            wrapper.eq(SecondhandItem::getStatus, status.trim());
        } else {
            wrapper.eq(SecondhandItem::getStatus, "1");
        }
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

    @Override
    public SecondhandMarketStatsResponse getMarketStats() {
        LambdaQueryWrapper<SecondhandItem> onSale = new LambdaQueryWrapper<>();
        onSale.eq(SecondhandItem::getStatus, "1");
        long total = itemRepository.selectCount(onSale);

        LocalDate today = LocalDate.now(CN_ZONE);
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        LambdaQueryWrapper<SecondhandItem> week = new LambdaQueryWrapper<>();
        week.eq(SecondhandItem::getStatus, "1")
                .ge(SecondhandItem::getCreateTime, weekStart);
        long newThisWeek = itemRepository.selectCount(week);

        return new SecondhandMarketStatsResponse(total, newThisWeek);
    }

    @Override
    @Transactional
    public SecondhandItemResponse approveListing(Long id, String operatorRole) {
        if (!Roles.isPlatformAdmin(operatorRole)) {
            throw new BusinessException(403, "仅店长或超级管理员可审批上架");
        }
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!"2".equals(item.getStatus())) {
            throw new BusinessException(400, "仅待审核商品可通过上架审批");
        }
        item.setStatus("1");
        itemRepository.updateById(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public SecondhandItemResponse rejectListing(Long id, String operatorRole) {
        if (!Roles.isPlatformAdmin(operatorRole)) {
            throw new BusinessException(403, "仅店长或超级管理员可驳回上架");
        }
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!"2".equals(item.getStatus())) {
            throw new BusinessException(400, "仅待审核商品可驳回");
        }
        item.setStatus("0");
        itemRepository.updateById(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public SecondhandItemResponse submitForListing(Long id, Long userId) {
        SecondhandItem item = itemRepository.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!userId.equals(item.getUserId())) {
            throw new BusinessException(403, "无权限操作");
        }
        if (!"0".equals(item.getStatus())) {
            throw new BusinessException(400, "仅下架或被驳回的商品可重新提交审核");
        }
        item.setStatus("2");
        itemRepository.updateById(item);
        return toResponse(item);
    }

    private void assertSecondhandReadable(SecondhandItem item, Long viewerUserId, String viewerRole) {
        if ("1".equals(item.getStatus())) {
            return;
        }
        if (Roles.isPlatformAdmin(viewerRole)) {
            return;
        }
        if (viewerUserId != null && viewerUserId.equals(item.getUserId())) {
            return;
        }
        throw new BusinessException(404, "商品不存在");
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
        response.setOriginalPrice(item.getOriginalPrice());
        response.setCondition(item.getCondition());
        String cover = item.getImage();
        if (!StringUtils.hasText(cover)) {
            cover = firstImageFromJson(item.getImages());
        }
        response.setImage(cover);
        response.setImages(item.getImages());
        response.setStatus(item.getStatus());
        response.setViewCount(item.getViewCount());
        response.setExpireTime(item.getExpireTime());
        response.setCreateTime(item.getCreateTime());
        response.setLocation(item.getLocation());

        String label = item.getUserId() != null ? userLookupRepository.findSellerLabelById(item.getUserId()) : null;
        response.setUserName(buildSellerDisplay(item.getUserId(), item.getContact(), label));
        response.setIntegrityTag(item.getUserId() != null && item.getUserId() % 3 != 0);
        return response;
    }

    private String firstImageFromJson(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return null;
        }
        try {
            List<String> urls = objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
            return urls.isEmpty() ? null : urls.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSellerDisplay(Long userId, String contact, String realNameOrUsername) {
        if (StringUtils.hasText(realNameOrUsername)
                && (realNameOrUsername.endsWith("女士") || realNameOrUsername.endsWith("先生"))) {
            return realNameOrUsername;
        }
        if (StringUtils.hasText(realNameOrUsername)) {
            char c = realNameOrUsername.charAt(0);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                String suffix = (userId != null && userId % 2 == 0) ? "女士" : "先生";
                return c + suffix;
            }
        }
        if (StringUtils.hasText(contact) && contact.length() >= 7 && contact.chars().allMatch(Character::isDigit)) {
            return contact.substring(0, 3) + "****" + contact.substring(contact.length() - 4);
        }
        return "居友" + (userId != null ? userId : "");
    }
}
