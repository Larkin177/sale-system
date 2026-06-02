package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingService siteSettingService;

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
}
