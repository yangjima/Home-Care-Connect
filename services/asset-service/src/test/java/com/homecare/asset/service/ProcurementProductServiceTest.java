package com.homecare.asset.service;

import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.ProcurementMallStatsResponse;
import com.homecare.asset.dto.ProcurementProductRequest;
import com.homecare.asset.dto.ProcurementProductResponse;
import com.homecare.asset.entity.ProcurementProduct;
import com.homecare.asset.repository.ProcurementProductRepository;
import com.homecare.asset.service.impl.ProcurementProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 采购商品服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProcurementProductServiceTest {

    @Mock
    private ProcurementProductRepository productRepository;

    private ProcurementProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProcurementProductServiceImpl(productRepository);
    }

    @Nested
    @DisplayName("商品查询测试")
    class ProductQueryTests {

        @Test
        @DisplayName("获取商品详情成功")
        void getById_Success() {
            ProcurementProduct product = createTestProduct(1L, "清洁工具套装");
            when(productRepository.selectById(1L)).thenReturn(product);

            ProcurementProductResponse response = productService.getById(1L, null, null);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("清洁工具套装", response.getName());
            assertEquals("1", response.getStatus());
        }

        @Test
        @DisplayName("获取商品详情-不存在")
        void getById_NotFound() {
            when(productRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> productService.getById(999L, null, null));

            assertEquals(404, exception.getCode());
            assertEquals("商品不存在", exception.getMessage());
        }

        @Test
        @DisplayName("商城统计返回已上架数量")
        void getMallStats_returnsOnShelfTotal() {
            when(productRepository.selectCount(any())).thenReturn(7L);

            ProcurementMallStatsResponse stats = productService.getMallStats();

            assertEquals(7L, stats.getTotalOnShelf());
        }
    }

    @Nested
    @DisplayName("商品库存测试")
    class StockTests {

        @Test
        @DisplayName("增加库存成功")
        void updateStock_Increase() {
            ProcurementProduct product = createTestProduct(1L, "清洁工具套装");
            product.setStock(10);
            when(productRepository.selectById(1L)).thenReturn(product);
            doNothing().when(productRepository).updateById(any(ProcurementProduct.class));

            ProcurementProductResponse response = productService.updateStock(1L, 5);

            assertNotNull(response);
            verify(productRepository).updateById(argThat(p ->
                    p.getStock() == 15
            ));
        }

        @Test
        @DisplayName("减少库存成功")
        void updateStock_Decrease() {
            ProcurementProduct product = createTestProduct(1L, "清洁工具套装");
            product.setStock(10);
            when(productRepository.selectById(1L)).thenReturn(product);
            doNothing().when(productRepository).updateById(any(ProcurementProduct.class));

            ProcurementProductResponse response = productService.updateStock(1L, -3);

            assertNotNull(response);
            verify(productRepository).updateById(argThat(p ->
                    p.getStock() == 7
            ));
        }

        @Test
        @DisplayName("减少库存-库存不足")
        void updateStock_Insufficient() {
            ProcurementProduct product = createTestProduct(1L, "清洁工具套装");
            product.setStock(5);
            when(productRepository.selectById(1L)).thenReturn(product);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> productService.updateStock(1L, -10));

            assertEquals(400, exception.getCode());
            assertEquals("库存不足", exception.getMessage());
        }

        @Test
        @DisplayName("更新库存-商品不存在")
        void updateStock_NotFound() {
            when(productRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> productService.updateStock(999L, 5));

            assertEquals(404, exception.getCode());
        }
    }

    @Nested
    @DisplayName("商品删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除商品成功")
        void delete_Success() {
            ProcurementProduct product = createTestProduct(1L, "清洁工具套装");
            when(productRepository.selectById(1L)).thenReturn(product);
            doNothing().when(productRepository).deleteById(1L);

            assertDoesNotThrow(() -> productService.delete(1L));

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("删除商品-不存在")
        void delete_NotFound() {
            when(productRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> productService.delete(999L));

            assertEquals(404, exception.getCode());
        }
    }

    // ============ 辅助方法 ============

    private ProcurementProduct createTestProduct(Long id, String name) {
        ProcurementProduct product = new ProcurementProduct();
        product.setId(id);
        product.setName(name);
        product.setDescription("测试描述");
        product.setCategory("清洁用品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStock(100);
        product.setUnit("套");
        product.setStoreId(1L);
        product.setStatus("1");
        product.setDeleted(0);
        product.setCreateTime(LocalDateTime.now());
        return product;
    }
}
