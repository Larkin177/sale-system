package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.Order;
import com.sales.entity.ProductPackage;
import com.sales.entity.SystemConfig;
import com.sales.entity.Product;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductPackageMapper;
import com.sales.mapper.SystemConfigMapper;
import com.sales.mapper.ProductMapper;
import com.sales.service.AlipayService;
import com.sales.service.AuthCodeService;
import com.sales.service.CommissionService;
import com.sales.service.ConfigService;
import com.sales.service.EmailService;
import com.sales.service.OrderService;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final OrderService orderService;
    private final CommissionService commissionService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final AlipayService alipayService;
    private final AuthCodeService authCodeService;
    private final EmailService emailService;
    private final ProductPackageMapper productPackageMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final SiteSettingService siteSettingService;
    private final ConfigService configService;

    @PostMapping("/wechat")
    public String wechatNotify(@RequestBody Map<String, Object> params) {
        log.info("微信支付回调: {}", params);

        try {
            // 1. 基本参数校验
            String orderNo = (String) params.get("out_trade_no");
            if (orderNo == null || orderNo.isEmpty()) {
                log.error("微信回调缺少订单号");
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[缺少订单号]]></return_msg></xml>";
            }

            String transactionId = (String) params.get("transaction_id");
            if (transactionId == null || transactionId.isEmpty()) {
                log.error("微信回调缺少交易号");
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[缺少交易号]]></return_msg></xml>";
            }

            // 2. 查询订单
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                log.error("订单不存在: {}", orderNo);
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[订单不存在]]></return_msg></xml>";
            }

            // 3. 检查订单状态，防止重复处理
            if (!"pending".equals(order.getStatus())) {
                log.warn("订单状态不是pending，跳过处理: {}", orderNo);
                return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            }

            // 4. 验证金额（防止篡改）
            Object totalFeeObj = params.get("total_fee");
            if (totalFeeObj != null) {
                String totalFee = String.valueOf(totalFeeObj);
                java.math.BigDecimal callbackAmount = new java.math.BigDecimal(totalFee).divide(new java.math.BigDecimal("100"));
                if (callbackAmount.compareTo(order.getAmount()) != 0) {
                    log.error("支付金额不匹配: 订单={}, 回调={}", order.getAmount(), callbackAmount);
                    return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[金额不匹配]]></return_msg></xml>";
                }
            }

            // 5. 更新订单状态
            order.setStatus("paid");
            order.setPaymentNo(transactionId);
            order.setPaymentMethod("wechat");
            order.setPaidAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 6. 计算分润
            commissionService.calculateCommission(order.getId());

            // 7. 自动发货（生成授权码）
            autoDeliver(order);

            log.info("微信支付回调处理成功，订单号: {}", orderNo);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";

        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }

    @PostMapping("/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝回调: {}", params);

        try {
            // 1. 验证签名
            if (!alipayService.verifyNotify(params)) {
                log.error("支付宝签名验证失败");
                return "fail";
            }

            // 2. 获取订单号
            String orderNo = params.get("out_trade_no");
            if (orderNo == null || orderNo.isEmpty()) {
                log.error("支付宝回调缺少订单号");
                return "fail";
            }

            // 3. 查询订单
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                log.error("订单不存在: {}", orderNo);
                return "fail";
            }

            // 4. 检查订单状态，防止重复处理
            if (!"pending".equals(order.getStatus())) {
                log.warn("订单状态不是pending，跳过处理: {}", orderNo);
                return "success";
            }

            // 5. 更新订单状态
            order.setStatus("paid");
            order.setPaymentNo(params.get("trade_no"));
            order.setPaymentMethod("alipay");
            order.setPaidAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 6. 计算分润
            commissionService.calculateCommission(order.getId());

            // 7. 自动发货（生成授权码）
            autoDeliver(order);

            log.info("支付宝支付回调处理成功，订单号: {}", orderNo);
            return "success";

        } catch (Exception e) {
            log.error("处理支付宝支付回调失败", e);
            return "fail";
        }
    }

    /**
     * 自动发货 - 生成授权码
     */
    private void autoDeliver(Order order) {
        try {
            // 如果订单没有产品ID，查找默认产品
            if (order.getProductId() == null) {
                order.setProductId(1L);
            }

            // 获取产品配置的授权有效期
            int validityHours = 72;
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                if (product.getAuthValidityHours() != null) {
                    validityHours = product.getAuthValidityHours();
                }
                order.setProductName(product.getName());
            }

            // 获取套餐下载链接
            String downloadUrl = "";
            ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;
            if (pkg != null && pkg.getDownloadUrl() != null && !pkg.getDownloadUrl().isEmpty()) {
                downloadUrl = pkg.getDownloadUrl();
            }

            // 生成授权码
            String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

            // 更新订单的授权信息
            order.setAuthCode(authCode);
            order.setAuthStatus("active");
            order.setStatus("delivered");
            orderMapper.updateById(order);

            // 保存到 license_codes 表（用于核销）
            authCodeService.saveLicenseCode(order.getId(), authCode);

            // 发送邮件通知
            sendDeliveryEmail(order, downloadUrl, authCode);

            log.info("自动发货成功，订单号: {}, 授权码: {}", order.getOrderNo(), authCode);
        } catch (Exception e) {
            log.error("自动发货失败，订单号: {}", order.getOrderNo(), e);
        }
    }

    private void sendDeliveryEmail(Order order, String downloadUrl, String authCode) {
        String email = order.getCustomerEmail();
        if (email == null || email.isEmpty()) return;

        String siteUrl = "http://localhost:3000";
        SystemConfig sc = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "site_url"));
        if (sc != null && sc.getConfigValue() != null && !sc.getConfigValue().isEmpty()) {
            siteUrl = sc.getConfigValue();
        }

        // 使用可配置的 delivery_template
        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\"><h2 style=\"color:#667eea;\">{product_name} - 订单已发货</h2><p>感谢您的购买！</p><p><strong>订单号：</strong>{order_no}</p><p><strong>授权码：</strong><code style=\"background:#f3f4f6;padding:4px 8px;border-radius:4px;\">{auth_code}</code></p><p>🔐 授权码有效期 {hours} 小时，一机一码</p><a href=\"{download_url}\" style=\"display:inline-block;padding:12px 24px;background:#667eea;color:white;text-decoration:none;border-radius:8px;margin:16px 0;\">📥 下载安装器</a></div>";
        }

        int validityHours = 72;
        ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;
        if (pkg != null && pkg.getAuthValidityHours() != null) {
            validityHours = pkg.getAuthValidityHours();
        }

        String orderUrl = siteUrl + "/orders?orderNo=" + order.getOrderNo();
        String content = template
                .replace("{site_name}", siteSettingService.getSetting("site_name") != null ? siteSettingService.getSetting("site_name") : "CC-Installer")
                .replace("{site_url}", siteUrl)
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{email}", email)
                .replace("{download_url}", downloadUrl != null && !downloadUrl.isEmpty() ? downloadUrl : orderUrl)
                .replace("{order_url}", orderUrl)
                .replace("{auth_code}", authCode != null ? authCode : "")
                .replace("{package_name}", order.getPackageName() != null ? order.getPackageName() : "")
                .replace("{product_name}", order.getProductName() != null ? order.getProductName() : "")
                .replace("{hours}", String.valueOf(validityHours));

        emailService.sendEmail(email, "您的订单已发货", content);
    }
}