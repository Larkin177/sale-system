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
        Map<String, String> publicConfig = new java.util.HashMap<>();
        // 价格由套餐管理控制，不再暴露系统级价格配置
        publicConfig.put("product_page_title", config.get("product_page_title"));
        publicConfig.put("product_page_subtitle", config.get("product_page_subtitle"));
        publicConfig.put("product_page_tips", config.get("product_page_tips"));
        publicConfig.put("default_commission_rate", config.get("default_commission_rate"));
        publicConfig.put("qrcode_logo", config.get("qrcode_logo"));
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
