package com.homecare.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.property.common.BusinessException;
import com.homecare.property.dto.PropertyCreateRequest;
import com.homecare.property.dto.PropertyResponse;
import com.homecare.property.entity.Property;
import com.homecare.property.entity.PropertyImage;
import com.homecare.property.repository.PropertyImageRepository;
import com.homecare.property.repository.PropertyRepository;
import com.homecare.property.repository.PropertyViewingRepository;
import com.homecare.property.repository.StoreRepository;
import com.homecare.property.service.impl.PropertyServiceImpl;
import com.homecare.property.util.Roles;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 房产服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private PropertyViewingRepository propertyViewingRepository;

    @Mock
    private StoreRepository storeRepository;

    private PropertyServiceImpl propertyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        propertyService = new PropertyServiceImpl(
                propertyRepository, propertyImageRepository, propertyViewingRepository, storeRepository, objectMapper
        );
    }

    @Nested
    @DisplayName("房产查询测试")
    class PropertyQueryTests {

        @Test
        @DisplayName("获取房产详情成功")
        void getPropertyById_Success() {
            Property property = createTestProperty(1L, "精装公寓", "vacant");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.getPropertyById(1L, null, null, null);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("精装公寓", response.getTitle());
            assertEquals("vacant", response.getStatus());
        }

        @Test
        @DisplayName("待审核房源对匿名访客不可见")
        void getPropertyById_pendingHiddenFromPublic() {
            Property property = createTestProperty(1L, "待审房", "pending");
            when(propertyRepository.selectById(1L)).thenReturn(property);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> propertyService.getPropertyById(1L, null, null, null));

            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("获取房产详情-房产不存在")
        void getPropertyById_NotFound() {
            when(propertyRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> propertyService.getPropertyById(999L, null, null, null));

            assertEquals(404, exception.getCode());
            assertEquals("房产不存在", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("房产状态测试")
    class PropertyStatusTests {

        @Test
        @DisplayName("平台管理员发布房产直接上架")
        void publishProperty_AdminToVacant() {
            Property property = createTestProperty(1L, "精装公寓", "pending");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.publishProperty(1L, 1L, Roles.ADMIN);

            assertNotNull(response);
            verify(propertyRepository).updateById(argThat(p -> {
                assertEquals("vacant", p.getStatus());
                assertNull(p.getPublishedAt());
                return true;
            }));
        }

        @Test
        @DisplayName("商家提交上架进入待审核")
        void publishProperty_MerchantToPending() {
            Property property = createTestProperty(1L, "精装公寓", "reserved");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            propertyService.publishProperty(1L, 1L, Roles.MERCHANT);

            verify(propertyRepository).updateById(argThat(p -> "pending".equals(p.getStatus())));
        }

        @Test
        @DisplayName("审批通过待审房源上架")
        void approvePropertyListing_Success() {
            Property property = createTestProperty(1L, "精装公寓", "pending");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            propertyService.approvePropertyListing(1L, 2L, Roles.STORE_MANAGER);

            verify(propertyRepository).updateById(argThat(p -> "vacant".equals(p.getStatus())));
        }

        @Test
        @DisplayName("下架房产成功")
        void offlineProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "vacant");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.offlineProperty(1L, 1L, Roles.ADMIN);

            assertNotNull(response);
            verify(propertyRepository).updateById(argThat(p ->
                    "reserved".equals(p.getStatus())
            ));
        }

        @Test
        @DisplayName("推荐房产成功")
        void recommendProperty_Success() {
            Property property = createTestProperty(1L, "精装公寓", "vacant");
            property.setIsRecommended(false);
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PropertyResponse response = propertyService.recommendProperty(1L, true, 1L, Roles.ADMIN);

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
            Property property = createTestProperty(1L, "精装公寓", "vacant");
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
            Property property = createTestProperty(1L, "精装公寓", "vacant");
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
            Property property = createTestProperty(1L, "精装公寓", "vacant");
            when(propertyRepository.selectById(1L)).thenReturn(property);
            doNothing().when(propertyRepository).deleteById(1L);
            when(propertyImageRepository.delete(any(LambdaQueryWrapper.class)))
                    .thenReturn(1);

            assertDoesNotThrow(() -> propertyService.deleteProperty(1L, 1L, Roles.ADMIN));

            verify(propertyRepository).deleteById(1L);
            verify(propertyImageRepository).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("删除房产-不存在")
        void deleteProperty_NotFound() {
            when(propertyRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> propertyService.deleteProperty(999L, 1L, Roles.ADMIN));

            assertEquals(404, exception.getCode());
        }
    }

    @Nested
    @DisplayName("房源媒体")
    class MediaTests {

        @Test
        @DisplayName("更新房源时删除旧媒体并写入新记录")
        void updateProperty_replacesMediaRows() {
            Property existing = createTestProperty(1L, "精装公寓", "vacant");
            when(propertyRepository.selectById(1L)).thenReturn(existing);
            doNothing().when(propertyRepository).updateById(any(Property.class));
            when(propertyImageRepository.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            doNothing().when(propertyImageRepository).insert(any(PropertyImage.class));

            PropertyImage rowCover = new PropertyImage();
            rowCover.setUrl("/minio/bucket/cover.jpg");
            rowCover.setType("cover");
            PropertyImage rowDetail = new PropertyImage();
            rowDetail.setUrl("/minio/bucket/other.jpg");
            rowDetail.setType("detail");
            PropertyImage rowVideo = new PropertyImage();
            rowVideo.setUrl("/minio/bucket/clips/a.mp4");
            rowVideo.setType("detail");
            when(propertyImageRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(rowCover, rowDetail, rowVideo));

            PropertyCreateRequest request = new PropertyCreateRequest();
            request.setTitle("精装公寓");
            request.setPropertyType("公寓");
            request.setRentPrice(new BigDecimal("3000"));
            request.setAddress("测试地址");
            request.setArea(new BigDecimal("80"));
            request.setImages(Arrays.asList("/minio/bucket/other.jpg", "/minio/bucket/cover.jpg"));
            request.setVideos(Collections.singletonList("/minio/bucket/clips/a.mp4"));
            request.setCoverImage("/minio/bucket/cover.jpg");

            PropertyResponse response = propertyService.updateProperty(1L, request, 1L, Roles.ADMIN);

            assertNotNull(response);
            verify(propertyImageRepository).delete(any(LambdaQueryWrapper.class));
            verify(propertyImageRepository, times(3)).insert(any(PropertyImage.class));
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
