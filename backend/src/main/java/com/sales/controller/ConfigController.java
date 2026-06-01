package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // 公开接口 - 获取基础配置（客户端使用）
    @GetMapping("/config")
    public ApiResponse<Map<String, String>> getPublicConfig() {
        Map<String, String> config = configService.getAllConfig();
        // 只返回客户端需要的配置
        Map<String, String> publicConfig = new java.util.HashMap<>();
        publicConfig.put("base_price", config.get("base_price"));
        publicConfig.put("min_price", config.get("min_price"));
        publicConfig.put("max_price", config.get("max_price"));
        return ApiResponse.success(publicConfig);
    }

    // 管理端接口 - 获取所有配置
    @GetMapping("/admin/config")
    public ApiResponse<Map<String, String>> getAllConfig() {
        return ApiResponse.success(configService.getAllConfig());
    }

    @PutMapping("/admin/config/{key}")
    public ApiResponse<Void> updateConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        configService.updateConfig(key, body.get("value"));
        return ApiResponse.success();
    }
}
