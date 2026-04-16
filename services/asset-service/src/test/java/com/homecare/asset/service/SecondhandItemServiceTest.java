package com.homecare.asset.service;

import com.homecare.asset.common.BusinessException;
import com.homecare.asset.common.PageResult;
import com.homecare.asset.dto.SecondhandItemRequest;
import com.homecare.asset.dto.SecondhandItemResponse;
import com.homecare.asset.entity.SecondhandItem;
import com.homecare.asset.repository.SecondhandItemRepository;
import com.homecare.asset.service.impl.SecondhandItemServiceImpl;
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
 * 二手商品服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class SecondhandItemServiceTest {

    @Mock
    private SecondhandItemRepository itemRepository;

    private SecondhandItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new SecondhandItemServiceImpl(itemRepository);
    }

    @Nested
    @DisplayName("商品查询测试")
    class ItemQueryTests {

        @Test
        @DisplayName("获取商品详情成功")
        void getById_Success() {
            SecondhandItem item = createTestItem(1L, "二手洗衣机");
            when(itemRepository.selectById(1L)).thenReturn(item);

            SecondhandItemResponse response = itemService.getById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("二手洗衣机", response.getTitle());
            assertEquals("9成新", response.getCondition());
        }

        @Test
        @DisplayName("获取商品详情-不存在")
        void getById_NotFound() {
            when(itemRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> itemService.getById(999L));

            assertEquals(404, exception.getCode());
        }
    }

    @Nested
    @DisplayName("浏览次数测试")
    class ViewCountTests {

        @Test
        @DisplayName("增加浏览次数成功")
        void incrementViewCount_Success() {
            SecondhandItem item = createTestItem(1L, "二手洗衣机");
            item.setViewCount(10L);
            when(itemRepository.selectById(1L)).thenReturn(item);
            doNothing().when(itemRepository).updateById(any(SecondhandItem.class));

            assertDoesNotThrow(() -> itemService.incrementViewCount(1L));

            verify(itemRepository).updateById(argThat(i ->
                    i.getViewCount() == 11
            ));
        }

        @Test
        @DisplayName("浏览次数为null时增加到1")
        void incrementViewCount_Null() {
            SecondhandItem item = createTestItem(1L, "二手洗衣机");
            item.setViewCount(null);
            when(itemRepository.selectById(1L)).thenReturn(item);
            doNothing().when(itemRepository).updateById(any(SecondhandItem.class));

            itemService.incrementViewCount(1L);

            verify(itemRepository).updateById(argThat(i ->
                    i.getViewCount() == 1
            ));
        }
    }

    @Nested
    @DisplayName("商品删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除商品成功")
        void delete_Success() {
            SecondhandItem item = createTestItem(1L, "二手洗衣机");
            when(itemRepository.selectById(1L)).thenReturn(item);
            doNothing().when(itemRepository).deleteById(1L);

            assertDoesNotThrow(() -> itemService.delete(1L, 1L));

            verify(itemRepository).deleteById(1L);
        }

        @Test
        @DisplayName("删除商品-无权限")
        void delete_NoPermission() {
            SecondhandItem item = createTestItem(1L, "二手洗衣机");
            when(itemRepository.selectById(1L)).thenReturn(item);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> itemService.delete(1L, 999L));

            assertEquals(403, exception.getCode());
        }

        @Test
        @DisplayName("删除商品-不存在")
        void delete_NotFound() {
            when(itemRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> itemService.delete(999L, 1L));

            assertEquals(404, exception.getCode());
        }
    }

    // ============ 辅助方法 ============

    private SecondhandItem createTestItem(Long id, String title) {
        SecondhandItem item = new SecondhandItem();
        item.setId(id);
        item.setUserId(1L);
        item.setStoreId(1L);
        item.setTitle(title);
        item.setDescription("测试描述");
        item.setCategory("家用电器");
        item.setPrice(new BigDecimal("500.00"));
        item.setCondition("9成新");
        item.setStatus("pending");
        item.setViewCount(0L);
        item.setExpireTime(LocalDateTime.now().plusDays(30));
        item.setDeleted(0);
        item.setCreateTime(LocalDateTime.now());
        return item;
    }
}
