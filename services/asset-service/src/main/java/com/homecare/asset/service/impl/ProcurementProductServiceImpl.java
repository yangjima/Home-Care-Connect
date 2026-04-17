package com.homecare.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.entity.ProcurementProduct;
import com.homecare.asset.repository.ProcurementProductRepository;
import com.homecare.asset.service.ProcurementProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 采购商品服务实现
 */
@Service
@RequiredArgsConstructor
public class ProcurementProductServiceImpl implements ProcurementProductService {

    private final ProcurementProductRepository productRepository;

    @Override
    @Transactional
    public ProcurementProductResponse create(ProcurementProductRequest request, Long storeId) {
        ProcurementProduct product = new ProcurementProduct();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setUnit(request.getUnit());
        product.setImage(request.getImage());
        product.setImages(request.getImages());
        product.setStoreId(storeId);
        product.setStatus("1");

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
        if (request.getImage() != null) product.setImage(request.getImage());
        if (request.getImages() != null) product.setImages(request.getImages());

        productRepository.updateById(product);
        return toResponse(product);
    }

    @Override
    public ProcurementProductResponse getById(Long id) {
        ProcurementProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return toResponse(product);
    }

    @Override
    public PageResult<ProcurementProductResponse> list(int page, int pageSize, String keyword,
            String category, String status) {
        LambdaQueryWrapper<ProcurementProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProcurementProduct::getName, keyword)
                   .or()
                   .like(ProcurementProduct::getDescription, keyword);
        }
        if (StringUtils.hasText(category)) wrapper.eq(ProcurementProduct::getCategory, category);
        if (StringUtils.hasText(status)) wrapper.eq(ProcurementProduct::getStatus, status);
        wrapper.orderByDesc(ProcurementProduct::getCreateTime);

        Page<ProcurementProduct> pageResult = productRepository.selectPage(
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
        response.setImage(product.getImage());
        response.setImages(product.getImages());
        response.setStoreId(product.getStoreId());
        response.setStatus(product.getStatus());
        response.setCreateTime(product.getCreateTime());
        return response;
    }
}
