package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Download;
import com.sales.entity.Order;
import com.sales.entity.ProductPackage;
import com.sales.mapper.DownloadMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductPackageMapper;
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
    private final ProductPackageMapper productPackageMapper;

    /**
     * 获取发货消息预览
     */
    @GetMapping("/admin/orders/{id}/delivery-preview")
    public ApiResponse<Map<String, String>> getDeliveryPreview(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        // 根据订单的 package_id 获取套餐信息
        ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;

        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = "感谢您的购买！订单号: {order_no}，金额: ¥{amount}\n授权码: {auth_code}";
        }

        String siteName = siteSettingService.getSetting("site_name");
        String token = generateDownloadToken(order);
        String downloadUrl = pkg != null && pkg.getDownloadUrl() != null && !pkg.getDownloadUrl().isEmpty()
                ? pkg.getDownloadUrl()
                : "http://localhost:3000/download?token=" + token;

        // 生成或使用已有授权码
        String authCode = order.getAuthCode();
        if (authCode == null || authCode.isEmpty()) {
            int validityHours = pkg != null && pkg.getAuthValidityHours() != null ? pkg.getAuthValidityHours() : 72;
            authCode = authCodeService.generateAuthCode(order.getId(), validityHours);
        }

        String message = template
                .replace("{site_name}", siteName != null ? siteName : "系统")
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                .replace("{download_url}", downloadUrl)
                .replace("{auth_code}", authCode)
                .replace("{product_name}", order.getProductName() != null ? order.getProductName() : "")
                .replace("{package_name}", order.getPackageName() != null ? order.getPackageName() : "")
                .replace("{platform}", order.getPlatform() != null ? order.getPlatform() : "")
                .replace("{version}", pkg != null && pkg.getVersion() != null ? pkg.getVersion() : "");

        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        result.put("phone", order.getCustomerPhone());
        result.put("orderNo", order.getOrderNo());
        result.put("downloadToken", token);
        result.put("authCode", authCode);
        if (pkg != null) {
            result.put("downloadUrl", downloadUrl);
            result.put("packageName", pkg.getName());
            result.put("platform", pkg.getPlatform());
        }
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

        // 根据订单的 package_id 获取套餐信息
        ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;

        // 生成下载token
        String token = generateDownloadToken(order);

        // 生成授权码（使用套餐配置的有效期）
        int validityHours = pkg != null && pkg.getAuthValidityHours() != null ? pkg.getAuthValidityHours() : 72;
        String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

        // 获取下载链接（优先使用套餐配置的链接）
        String downloadUrl = pkg != null && pkg.getDownloadUrl() != null && !pkg.getDownloadUrl().isEmpty()
                ? pkg.getDownloadUrl()
                : "http://localhost:3000/download?token=" + token;

        // 保存授权码到订单
        order.setAuthCode(authCode);
        order.setAuthStatus("active");
        if (order.getProductId() == null && pkg != null) {
            order.setProductId(pkg.getProductId());
        }
        orderMapper.updateById(order);

        // 构建发货消息
        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = "感谢您的购买！订单号: {order_no}，金额: ¥{amount}\n授权码: {auth_code}";
        }

        String siteName = siteSettingService.getSetting("site_name");

        String message = template
                .replace("{site_name}", siteName != null ? siteName : "系统")
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                .replace("{download_url}", downloadUrl)
                .replace("{auth_code}", authCode)
                .replace("{product_name}", order.getProductName() != null ? order.getProductName() : "")
                .replace("{package_name}", order.getPackageName() != null ? order.getPackageName() : "")
                .replace("{platform}", order.getPlatform() != null ? order.getPlatform() : "")
                .replace("{version}", pkg != null && pkg.getVersion() != null ? pkg.getVersion() : "");

        log.info("[MOCK SMS] 发送发货消息 - 订单: {}, 手机: {}, 套餐: {}, 消息: {}",
                order.getOrderNo(), order.getCustomerPhone(), order.getPackageName(), message);

        Map<String, String> result = new HashMap<>();
        result.put("status", "sent");
        result.put("message", "发货消息已发送（开发环境仅模拟）");
        result.put("phone", order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
        result.put("downloadToken", token);
        result.put("authCode", authCode);
        result.put("downloadUrl", downloadUrl);
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
