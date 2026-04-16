package com.homecare.property.service;

import java.util.Map;

/**
 * MinIO 文件存储服务接口
 */
public interface MinioService {

    /**
     * 上传文件
     *
     * @param bucket 存储桶名称
     * @param objectName 对象名称
     * @param data 文件数据
     * @param contentType 内容类型
     * @return 访问URL
     */
    String uploadFile(String bucket, String objectName, byte[] data, String contentType);

    /**
     * 获取文件访问URL
     */
    String getFileUrl(String bucket, String objectName);

    /**
     * 删除文件
     */
    void deleteFile(String bucket, String objectName);

    /**
     * 生成预签名上传URL
     */
    String generatePresignedUploadUrl(String bucket, String objectName, int expiresSeconds);

    /**
     * 生成预签名下载URL
     */
    String generatePresignedDownloadUrl(String bucket, String objectName, int expiresSeconds);
}
