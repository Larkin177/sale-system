package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.entity.Download;
import com.sales.mapper.DownloadMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadMapper downloadMapper;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getDownloadInfo(@RequestParam String token) {
        Download download = downloadMapper.selectOne(
                new LambdaQueryWrapper<Download>()
                        .eq(Download::getDownloadToken, token));

        if (download == null) {
            return ApiResponse.error("下载链接无效");
        }

        // 检查是否过期
        if (download.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            return ApiResponse.error("下载链接已过期");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("expireAt", download.getExpireAt());
        result.put("downloadCount", download.getDownloadCount());
        result.put("orderId", download.getOrderId());

        return ApiResponse.success(result);
    }
}
