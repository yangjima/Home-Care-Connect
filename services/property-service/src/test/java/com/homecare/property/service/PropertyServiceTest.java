package com.homecare.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.property.common.BusinessException;
import com.homecare.property.dto.PropertyCreateRequest;
import com.homecare.property.dto.PropertyResponse;
import com.homecare.property.entity.Property;
import com.homecare.property.entity.PropertyImage;
import com.homecare.property.repository.PropertyImageRepository;
import com.homecare.property.repository.PropertyRepository;
import com.homecare.property.service.impl.PropertyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 房产服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    private PropertyServiceImpl propertyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        propertyService = new PropertyServiceImpl(
                propertyRepository, propertyImageRepository, objectMapper
        );
    }

    @Nested
    @DisplayName("房产查询测试")
    class PropertyQueryTests {

        @Test
        @DisplayName("获取房产详情成功")
        void getPropertyById_Success() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.getPropertyById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("精装公寓", response.getTitle());
            assertEquals("published", response.getStatus());
        }

        @Test
        @DisplayName("获取房产详情-房产不存在")
        void getPropertyById_NotFound() {
            when(propertyRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> propertyService.getPropertyById(999L));

            assertEquals(404, exception.getCode());
            assertEquals("房产不存在", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("房产状态测试")
    class PropertyStatusTests {

        @Test
        @DisplayName("发布房产成功")
        void publishProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "draft");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.publishProperty(1L);

            assertNotNull(response);
            verify(propertyRepository).updateById(argThat(p -> {
                assertEquals("published", p.getStatus());
                assertNotNull(p.getPublishedAt());
                return true;
            }));
        }

        @Test
        @DisplayName("下架房产成功")
        void offlineProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.offlineProperty(1L);

            assertNotNull(response);
            verify(propertyRepository).updateById(argThat(p ->
                    "offline".equals(p.getStatus())
            ));
        }

        @Test
        @DisplayName("推荐房产成功")
        void recommendProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            property.setIsRecommended(false);
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.recommendProperty(1L, true);

            verify(propertyRepository).updateById(argThat(p ->
                    p.getIsRecommended()
            ));
        }
    }

    @Nested
    @DisplayName("浏览次数测试")
    class ViewCountTests {

        @Test
        @DisplayName("增加浏览次数成功")
        void incrementViewCount_Success() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            property.setViewCount(10);
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));

            assertDoesNotThrow(() -> propertyService.incrementViewCount(1L));

            verify(propertyRepository).updateById(argThat(p ->
                    p.getViewCount() == 11
            ));
        }

        @Test
        @DisplayName("浏览次数为null时增加到1")
        void incrementViewCount_Null() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            property.setViewCount(null);
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));

            propertyService.incrementViewCount(1L);

            verify(propertyRepository).updateById(argThat(p ->
                    p.getViewCount() == 1
            ));
        }
    }

    @Nested
    @DisplayName("房产删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除房产成功")
        void deleteProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "published");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).deleteById(1L);
            when(propertyImageRepository.delete(any(LambdaQueryWrapper.class)))
                    .thenReturn(1);

            assertDoesNotThrow(() -> propertyService.deleteProperty(1L));

            verify(propertyRepository).deleteById(1L);
            verify(propertyImageRepository).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("删除房产-不存在")
        void deleteProperty_NotFound() {
            when(propertyRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> propertyService.deleteProperty(999L));

            assertEquals(404, exception.getCode());
        }
    }

    // ============ 辅助方法 ============

    private Property createTestProperty(Long id, String title, String status) {
        Property property = new Property();
        property.setId(id);
        property.setStoreId(1L);
        property.setOwnerId(1L);
        property.setTitle(title);
        property.setDescription("测试描述");
        property.setPropertyType("apartment");
        property.setRentPrice(new BigDecimal("3000"));
        property.setDeposit(new BigDecimal("6000"));
        property.setAddress("测试地址");
        property.setDistrict("天河区");
        property.setCity("广州");
        property.setArea(new BigDecimal("80"));
        property.setLayout("2室1厅1卫");
        property.setFloor(10);
        property.setTotalFloor(30);
        property.setOrientation("south");
        property.setStatus(status);
        property.setViewCount(0);
        property.setIsRecommended(false);
        property.setDeleted(0);
        property.setCreateTime(LocalDateTime.now());
        return property;
    }
}
