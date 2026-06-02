package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final SiteSettingService siteSettingService;

    @GetMapping("/payment/qrcode")
    public ApiResponse<Map<String, Object>> getPaymentQrCode(@RequestParam String method) {
        String key = "wechat".equals(method) ? "wechat_qrcode" : "alipay_qrcode";
        String qrcode = siteSettingService.getSetting(key);
        Map<String, Object> result = new HashMap<>();
        result.put("qrcode", qrcode != null ? qrcode : "");
        return ApiResponse.success(result);
    }
}
