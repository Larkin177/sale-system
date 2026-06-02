package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Download;
import com.sales.entity.Order;
import com.sales.mapper.DownloadMapper;
import com.sales.mapper.OrderMapper;
import com.sales.service.AuthCodeService;
import com.sales.service.ConfigService;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeliveryController {

    private final OrderMapper orderMapper;
    private final DownloadMapper downloadMapper;
    private final ConfigService configService;
    private final SiteSettingService siteSettingService;
    private final AuthCodeService authCodeService;

    /**
     * 获取发货消息预览
     */
    @GetMapping("/admin/orders/{id}/delivery-preview")
    public ApiResponse<Map<String, String>> getDeliveryPreview(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = "感谢您的购买！订单号: {order_no}，金额: ¥{amount}";
        }

        String siteName = siteSettingService.getSetting("site_name");
        String token = generateDownloadToken(order);
        String downloadUrl = "http://localhost:3000/download?token=" + token;

        // Generate auth code for preview (or use existing one)
        String authCode = order.getAuthCode();
        if (authCode == null || authCode.isEmpty()) {
            authCode = authCodeService.generateAuthCode(order.getId(), 72);
        }

        String message = template
                .replace("{site_name}", siteName != null ? siteName : "系统")
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                .replace("{download_url}", downloadUrl)
                .replace("{auth_code}", authCode);

        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        result.put("phone", order.getCustomerPhone());
        result.put("orderNo", order.getOrderNo());
        result.put("downloadToken", token);
        result.put("authCode", authCode);
        return ApiResponse.success(result);
    }

    /**
     * 发送发货消息（开发环境仅模拟，不实际发送短信）
     */
    @PostMapping("/admin/orders/{id}/send-delivery")
    public ApiResponse<Map<String, String>> sendDelivery(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        // 生成下载token
        String token = generateDownloadToken(order);

        // Generate auth code
        int validityHours = 72; // default, could be from config
        String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

        // Save auth code to order
        order.setAuthCode(authCode);
        order.setAuthStatus("active");
        order.setProductId(1L); // CC-Installer product ID
        orderMapper.updateById(order);

        // 构建发货消息（生产环境应调用短信API）
        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = "感谢您的购买！订单号: {order_no}，金额: ¥{amount}";
        }

        String siteName = siteSettingService.getSetting("site_name");
        String downloadUrl = "http://localhost:3000/download?token=" + token;

        String message = template
                .replace("{site_name}", siteName != null ? siteName : "系统")
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                .replace("{download_url}", downloadUrl)
                .replace("{auth_code}", authCode);

        log.info("[MOCK SMS] 发送发货消息 - 订单: {}, 手机: {}, 消息: {}",
                order.getOrderNo(), order.getCustomerPhone(), message);

        Map<String, String> result = new HashMap<>();
        result.put("status", "sent");
        result.put("message", "发货消息已发送（开发环境仅模拟）");
        result.put("phone", order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
        result.put("downloadToken", token);
        result.put("authCode", authCode);
        return ApiResponse.success(result);
    }

    /**
     * 生成或获取下载token
     * 如果订单已有下载记录则复用，否则创建新的
     */
    private String generateDownloadToken(Order order) {
        // 检查是否已有下载记录
        Download existing = downloadMapper.selectOne(
                new LambdaQueryWrapper<Download>()
                        .eq(Download::getOrderId, order.getId()));
        if (existing != null) {
            return existing.getDownloadToken();
        }

        // 创建新的下载记录
        Download download = new Download();
        download.setOrderId(order.getId());
        download.setDownloadToken("dl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        download.setDownloadCount(0);
        download.setExpireAt(LocalDateTime.now().plusDays(30));
        downloadMapper.insert(download);

        return download.getDownloadToken();
    }
}
