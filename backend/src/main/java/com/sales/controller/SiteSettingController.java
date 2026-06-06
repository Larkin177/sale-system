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
            String newFilename = UUID.randomUUID().toString() + ext;

            // 用 InputStream + Files.copy 保存，避免 file.transferTo 的路径问题
            Path filePath = uploadPath.resolve(newFilename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件上传成功: {}", filePath.toAbsolutePath());

            // 返回可访问的 URL
            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + newFilename);
            result.put("filename", originalFilename);
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
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
            lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
            return true;
        }
        return false;
    }
}
