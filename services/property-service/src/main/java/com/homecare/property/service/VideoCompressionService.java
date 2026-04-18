package com.homecare.property.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 房源视频上传前做轻度转码压缩（H.264/AAC MP4），失败时回退原文件。
 */
public interface VideoCompressionService {

    /**
     * @return 待写入对象存储的字节、Content-Type、对象名后缀（含点，如 .mp4）
     */
    PreparedVideo prepareForStorage(MultipartFile file) throws java.io.IOException;

    record PreparedVideo(byte[] data, String contentType, String filenameExtension) {}
}
