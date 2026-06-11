package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingService siteSettingService;

    @Value("${upload.dir}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建上传目录: {}", path.toAbsolutePath());
            }
            log.info("上传目录: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
        }
    }

    // 公开接口 - 获取客户页面展示的设置
    @GetMapping("/site-settings")
    public ApiResponse<Map<String, String>> getPublicSettings() {
        return ApiResponse.success(siteSettingService.getPublicSettings());
    }

    // 管理端接口 - 获取所有设置
    @GetMapping("/admin/site-settings")
    public ApiResponse<Map<String, String>> getAllSettings() {
        return ApiResponse.success(siteSettingService.getAllSettings());
    }

    // 管理端接口 - 批量更新设置
    @PutMapping("/admin/site-settings")
    public ApiResponse<Void> batchUpdate(@RequestBody Map<String, String> body) {
        siteSettingService.batchUpdate(body);
        return ApiResponse.success();
    }

    // 通用文件上传接口（支持图片、视频、文件）
    @PostMapping("/admin/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isAllowedFile(originalFilename)) {
                return ApiResponse.error(400, "不支持的文件格式");
            }

            // 验证文件大小 (最大 100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                return ApiResponse.error(400, "文件大小不能超过 100MB");
            }

            // 使用绝对路径
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            String baseName = originalFilename.lastIndexOf(".") > 0 
                ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) 
                : "file";
            String newFilename = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            // 用 InputStream + Files.copy 保存，避免 file.transferTo 的路径问题
            Path filePath = uploadPath.resolve(newFilename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件上传成功: {}", filePath.toAbsolutePath());

            // 返回可访问的 URL
            Map<String, String> result = new HashMap<>();
            result.put("url", "/downloads/" + newFilename);
            result.put("filename", originalFilename);

            // 如果是视频文件，自动生成缩略图
            String thumbFilename = null;
            String fileNameLower = originalFilename.toLowerCase();
            if (isVideoFile(fileNameLower)) {
                try {
                    thumbFilename = generateVideoThumbnail(filePath, newFilename);
                    if (thumbFilename != null) {
                        result.put("thumbnailUrl", "/downloads/" + thumbFilename);
                    }
                } catch (Exception e) {
                    log.warn("视频缩略图生成失败(不影响上传): {}", e.getMessage());
                }
            }

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }

    // 判断是否为视频文件
    private boolean isVideoFile(String lower) {
        return lower.endsWith(".mp4") || lower.endsWith(".avi") ||
               lower.endsWith(".mov") || lower.endsWith(".wmv") || lower.endsWith(".flv");
    }

    // 从视频提取第一帧生成缩略图（优先 FFmpeg，失败则用 JCodec）
    private String generateVideoThumbnail(Path videoPath, String videoFilename) {
        String thumbName = videoFilename.substring(0, videoFilename.lastIndexOf(".")) + ".jpg";
        Path thumbPath = videoPath.getParent().resolve(thumbName);

        // 方案1: 使用 FFmpeg（支持格式最全）
        try {
            // 优先使用项目目录下的 ffmpeg.exe，其次用系统 PATH 中的 ffmpeg
            String ffmpegCmd = "ffmpeg";
            Path localFfmpeg = Paths.get("ffmpeg.exe");
            if (Files.exists(localFfmpeg)) {
                ffmpegCmd = localFfmpeg.toAbsolutePath().toString();
            }
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegCmd, "-y", "-i", videoPath.toString(),
                "-vframes", "1", "-q:v", "2", thumbPath.toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(10, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0 && Files.exists(thumbPath) && Files.size(thumbPath) > 0) {
                log.info("视频缩略图生成成功(FFmpeg): {}", thumbPath.toAbsolutePath());
                return thumbName;
            }
            log.warn("FFmpeg 未生效，尝试 JCodec 方案");
        } catch (Exception e) {
            log.warn("FFmpeg 不可用: {}，尝试 JCodec", e.getMessage());
        }

        // 方案2: 使用 JCodec（纯 Java 兜底）
        try {
            Picture picture = FrameGrab.getFrameFromFile(videoPath.toFile(), 0);
            BufferedImage image = AWTUtil.toBufferedImage(picture);
            ImageIO.write(image, "jpg", thumbPath.toFile());
            log.info("视频缩略图生成成功(JCodec): {}", thumbPath.toAbsolutePath());
            return thumbName;
        } catch (Exception e) {
            log.warn("视频缩略图生成失败(两种方案均无效): {} - {}", videoFilename, e.getMessage());
            return null;
        }
    }

    // 验证文件类型
    private boolean isAllowedFile(String filename) {
        String lower = filename.toLowerCase();
        // 图片
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
            return true;
        }
        // 视频
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") ||
            lower.endsWith(".mov") || lower.endsWith(".wmv") || lower.endsWith(".flv")) {
            return true;
        }
        // 文件
        if (lower.endsWith(".zip") || lower.endsWith(".rar") ||
            lower.endsWith(".7z") || lower.endsWith(".pdf") ||
            lower.endsWith(".doc") || lower.endsWith(".docx") ||
            lower.endsWith(".xls") || lower.endsWith(".xlsx") ||
            lower.endsWith(".md") || lower.endsWith(".exe") ||
            lower.endsWith(".msi") || lower.endsWith(".dmg") ||
            lower.endsWith(".pkg")) {
            return true;
        }
        return false;
    }
}
