package com.homecare.property.service.impl;

import com.homecare.property.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * MinIO 文件存储服务实现
 */
@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.buckets.property:property-images}")
    private String propertyBucket;

    public MinioServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String uploadFile(String bucket, String objectName, byte[] data, String contentType) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(stream, data.length, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("文件上传成功: bucket={}, object={}", bucket, objectName);
            return getFileUrl(bucket, objectName);
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, object={}, error={}", bucket, objectName, e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String bucket, String objectName) {
        try {
            return minioClient.getObjectUrl(bucket, objectName);
        } catch (Exception e) {
            log.error("获取文件URL失败: bucket={}, object={}", bucket, objectName);
            throw new RuntimeException("获取文件URL失败", e);
        }
    }

    @Override
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, object={}", bucket, objectName);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public String generatePresignedUploadUrl(String bucket, String objectName, int expiresSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    Method.PUT, bucket, objectName, expiresSeconds, null
            );
        } catch (Exception e) {
            throw new RuntimeException("生成上传URL失败", e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String bucket, String objectName, int expiresSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    Method.GET, bucket, objectName, expiresSeconds, null
            );
        } catch (Exception e) {
            throw new RuntimeException("生成下载URL失败", e);
        }
    }

    /**
     * 上传房产图片的便捷方法
     */
    public String uploadPropertyImage(String objectName, byte[] data, String contentType) {
        return uploadFile(propertyBucket, objectName, data, contentType);
    }

    /**
     * 生成房产图片预签名上传URL
     */
    public String generatePropertyImageUploadUrl(String objectName, int expiresSeconds) {
        return generatePresignedUploadUrl(propertyBucket, objectName, expiresSeconds);
    }
}
