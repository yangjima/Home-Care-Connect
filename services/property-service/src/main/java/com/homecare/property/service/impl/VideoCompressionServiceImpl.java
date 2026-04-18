package com.homecare.property.service.impl;

import com.homecare.property.service.VideoCompressionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用系统 ffmpeg 做轻度压缩（CRF + faststart）。未安装或失败时返回原始字节。
 */
@Service
@Slf4j
public class VideoCompressionServiceImpl implements VideoCompressionService {

    private static final int FFMPEG_TIMEOUT_SECONDS = 600;

    @Override
    public PreparedVideo prepareForStorage(MultipartFile file) throws IOException {
        String origName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.bin";
        String inExt = extensionFromFilename(origName);
        if (inExt.isEmpty()) {
            inExt = ".bin";
        }
        String declaredType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        Path tempDir = Files.createTempDirectory("property-video-");
        byte[] originalBytes = null;
        try {
            Path input = tempDir.resolve("source" + inExt);
            file.transferTo(input);
            originalBytes = Files.readAllBytes(input);

            Path output = tempDir.resolve("compressed.mp4");
            byte[] transcoded = tryTranscodeToMp4(input, output);
            if (transcoded != null && transcoded.length > 0 && transcoded.length < originalBytes.length) {
                return new PreparedVideo(transcoded, "video/mp4", ".mp4");
            }
            if (transcoded != null && transcoded.length > 0) {
                log.debug("转码后体积未减小，使用原视频: orig={} trans={}", originalBytes.length, transcoded.length);
            }
            return new PreparedVideo(originalBytes, declaredType, inExt);
        } catch (Exception e) {
            log.warn("视频转码跳过，使用原文件: {}", e.getMessage());
            if (originalBytes != null) {
                return new PreparedVideo(originalBytes, declaredType, inExt);
            }
            return new PreparedVideo(file.getBytes(), declaredType, inExt);
        } finally {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
            } catch (IOException e) {
                log.warn("清理临时目录失败: {}", tempDir);
            }
        }
    }

    private byte[] tryTranscodeToMp4(Path input, Path output) throws IOException, InterruptedException {
        if (!runFfmpeg(buildFfmpegCommand(input, output, true))) {
            Files.deleteIfExists(output);
            if (!runFfmpeg(buildFfmpegCommand(input, output, false))) {
                return null;
            }
        }
        if (!Files.isRegularFile(output)) {
            return null;
        }
        return Files.readAllBytes(output);
    }

    private static List<String> buildFfmpegCommand(Path input, Path output, boolean withAudio) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-nostdin");
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("error");
        cmd.add("-i");
        cmd.add(input.toAbsolutePath().toString());
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-crf");
        cmd.add("26");
        cmd.add("-preset");
        cmd.add("faster");
        cmd.add("-movflags");
        cmd.add("+faststart");
        if (withAudio) {
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("96k");
        } else {
            cmd.add("-an");
        }
        cmd.add(output.toAbsolutePath().toString());
        return cmd;
    }

    private boolean runFfmpeg(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        boolean finished = p.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            log.warn("ffmpeg 超时");
            return false;
        }
        return p.exitValue() == 0;
    }

    private static String extensionFromFilename(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i).toLowerCase();
    }
}
