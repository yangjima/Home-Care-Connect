package com.homecare.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.entity.ProcurementProduct;
import com.homecare.asset.repository.ProcurementProductRepository;
import com.homecare.asset.service.ProcurementProductService;
import com.homecare.asset.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购商品服务实现
 */
@Service
@RequiredArgsConstructor
public class ProcurementProductServiceImpl implements ProcurementProductService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ProcurementProductRepository productRepository;

    @Override
    @Transactional
    public ProcurementProductResponse create(ProcurementProductRequest request, Long storeId, String operatorRole) {
        ProcurementProduct product = new ProcurementProduct();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit() : "件");
        if (StringUtils.hasText(request.getImages())) {
            product.setImages(request.getImages());
        } else if (StringUtils.hasText(request.getImage())) {
            try {
                product.setImages(JSON.writeValueAsString(List.of(request.getImage())));
            } catch (Exception e) {
                product.setImages(request.getImage());
            }
        }
        product.setSalesCount(request.getSalesCount() != null ? request.getSalesCount() : 0);
        product.setProductTag(request.getProductTag());
        product.setStoreId(storeId);
        product.setStatus(Roles.isPlatformAdmin(operatorRole) ? "1" : "2");

        productRepository.insert(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProcurementProductResponse update(Long id, ProcurementProductRequest request) {
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getUnit() != null) product.setUnit(request.getUnit());
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        } else if (StringUtils.hasText(request.getImage())) {
            try {
                product.setImages(JSON.writeValueAsString(List.of(request.getImage())));
            } catch (Exception e) {
                product.setImages(request.getImage());
            }
        }
        if (request.getSalesCount() != null) product.setSalesCount(request.getSalesCount());
        if (request.getProductTag() != null) product.setProductTag(request.getProductTag());

        productRepository.updateById(product);
        return toResponse(product);
    }

    @Override
    public ProcurementProductResponse getById(Long id, Long viewerUserId, String viewerRole) {
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if ("1".equals(product.getStatus())) {
            return toResponse(product);
        }
        if (Roles.isPlatformAdmin(viewerRole)) {
            return toResponse(product);
        }
        if (viewerUserId != null && viewerUserId.equals(product.getStoreId())) {
            return toResponse(product);
        }
        throw new BusinessException(404, "商品不存在");
    }

    @Override
    @Transactional
    public ProcurementProductResponse approveListing(Long id, String operatorRole) {
        if (!Roles.isPlatformAdmin(operatorRole)) {
            throw new BusinessException(403, "仅店长或超级管理员可审批上架");
        }
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!"2".equals(product.getStatus())) {
            throw new BusinessException(400, "仅待审核状态的商品可通过上架审批");
        }
        product.setStatus("1");
        productRepository.updateById(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProcurementProductResponse rejectListing(Long id, String operatorRole) {
        if (!Roles.isPlatformAdmin(operatorRole)) {
            throw new BusinessException(403, "仅店长或超级管理员可驳回上架");
        }
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!"2".equals(product.getStatus())) {
            throw new BusinessException(400, "仅待审核状态的商品可驳回");
        }
        product.setStatus("0");
        productRepository.updateById(product);
        return toResponse(product);
    }

    @Override
    public PageResult<ProcurementProductResponse> list(int page, int pageSize, String keyword,
            String category, String status, String sort, Long viewerUserId, String viewerRole) {
        LambdaQueryWrapper<ProcurementProduct> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ProcurementProduct::getName, kw)
                    .or()
                    .like(ProcurementProduct::getDescription, kw));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(ProcurementProduct::getCategory, category);
        }
        if (StringUtils.hasText(status) && Roles.isPlatformAdmin(viewerRole)) {
            wrapper.eq(ProcurementProduct::getStatus, status.trim());
        } else {
            wrapper.eq(ProcurementProduct::getStatus, "1");
        }

        if ("in_stock".equalsIgnoreCase(sort)) {
            wrapper.gt(ProcurementProduct::getStock, 0);
        }

        applySort(wrapper, sort);

        Page<ProcurementProduct> pageResult = productRepository.selectPage(
                new Page<>(page, pageSize), wrapper);

        return new PageResult<>(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    private void applySort(LambdaQueryWrapper<ProcurementProduct> wrapper, String sort) {
        if (!StringUtils.hasText(sort) || "newest".equalsIgnoreCase(sort)
                || "in_stock".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(ProcurementProduct::getCreateTime);
            return;
        }
        if ("sales".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(ProcurementProduct::getSalesCount)
                    .orderByDesc(ProcurementProduct::getCreateTime);
            return;
        }
        if ("price_asc".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(ProcurementProduct::getPrice)
                    .orderByDesc(ProcurementProduct::getCreateTime);
            return;
        }
        if ("price_desc".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(ProcurementProduct::getPrice)
                    .orderByDesc(ProcurementProduct::getCreateTime);
            return;
        }
        wrapper.orderByDesc(ProcurementProduct::getCreateTime);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ProcurementProductResponse updateStock(Long id, int quantity) {
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new BusinessException(400, "库存不足");
        }

        product.setStock(newStock);
        productRepository.updateById(product);
        return toResponse(product);
    }

    private ProcurementProductResponse toResponse(ProcurementProduct product) {
        ProcurementProductResponse response = new ProcurementProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setUnit(product.getUnit());
        response.setImages(product.getImages());
        response.setImage(firstImageFromJson(product.getImages()));
        response.setStoreId(product.getStoreId());
        response.setStatus(product.getStatus());
        response.setCreateTime(product.getCreateTime());
        response.setSalesCount(product.getSalesCount());
        response.setProductTag(product.getProductTag());
        return response;
    }

    private String firstImageFromJson(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(imagesJson.trim());
            if (root.isArray() && root.size() > 0 && root.get(0).isTextual()) {
                return root.get(0).asText();
            }
            if (root.isTextual()) {
                return root.asText();
            }
        } catch (Exception ignored) {
            // 非 JSON 时当作单个 URL
            if (!imagesJson.trim().startsWith("[")) {
                return imagesJson.trim();
            }
        }
        return null;
    }
}
