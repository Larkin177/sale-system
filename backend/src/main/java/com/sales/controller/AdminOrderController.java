package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.entity.ProductPackage;
import com.sales.entity.SystemConfig;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductMapper;
import com.sales.mapper.SystemConfigMapper;
import com.sales.mapper.ProductPackageMapper;
import com.sales.mapper.SalesMapper;
import com.sales.entity.Sales;
import com.sales.service.AuthCodeService;
import com.sales.service.CommissionService;
import com.sales.service.ConfigService;
import com.sales.service.EmailService;
import com.sales.service.OrderService;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final ProductPackageMapper productPackageMapper;
    private final CommissionService commissionService;
    private final AuthCodeService authCodeService;
    private final ConfigService configService;
    private final SiteSettingService siteSettingService;
    private final EmailService emailService;
    private final SystemConfigMapper systemConfigMapper;
    private final SalesMapper salesMapper;

    @GetMapping
    public ApiResponse<Page<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);
        // 填充销售姓名
        if (result.getRecords() != null) {
            for (Order o : result.getRecords()) {
                if (o.getSalesId() != null) {
                    Sales s = salesMapper.selectById(o.getSalesId());
                    if (s != null) o.setSalesName(s.getName());
                }
            }
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/unclaimed")
    public ApiResponse<List<Order>> unclaimed() {
        return orderService.listUnclaimedOrders();
    }

    /**
     * 管理员确认收款（静态支付模式）
     */
    @PostMapping("/{id}/confirm-payment")
    public ApiResponse<Map<String, String>> confirmPayment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = body.get("adminId") != null ? Long.valueOf(body.get("adminId")) : null;
        String note = body.getOrDefault("note", "");

        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        if (!"pending".equals(order.getStatus()) && !"pending_verify".equals(order.getStatus())) {
            return ApiResponse.error("订单状态不正确，当前状态: " + order.getStatus());
        }

        // 1. 标记已支付
        order.setStatus("paid");
        order.setPaymentMethod(body.getOrDefault("paymentMethod", "wechat"));
        order.setPaymentNo("MANUAL-" + System.currentTimeMillis());
        order.setPaidAt(LocalDateTime.now());
        order.setReviewedBy(adminId);
        order.setReviewNote(note);
        order.setReviewedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 2. 计算分润
        if (order.getSalesId() != null) {
            commissionService.calculateCommission(order.getId());
        }

        // 3. 自动发货：生成授权码
        autoDeliver(order);

        log.info("管理员确认收款 - 订单: {}, 操作人: {}", order.getOrderNo(), adminId);

        Map<String, String> result = Map.of("status", "delivered", "message", "确认收款成功，已自动发货");
        return ApiResponse.success(result);
    }

    /**
     * 管理员拒绝收款（静态支付模式）
     */
    @PostMapping("/{id}/reject-payment")
    public ApiResponse<Void> rejectPayment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = body.get("adminId") != null ? Long.valueOf(body.get("adminId")) : null;
        String note = body.getOrDefault("note", "");

        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        if (!"pending".equals(order.getStatus()) && !"pending_verify".equals(order.getStatus())) {
            return ApiResponse.error("订单状态不正确，当前状态: " + order.getStatus());
        }

        order.setReviewedBy(adminId);
        order.setReviewNote("已拒绝: " + note);
        order.setReviewedAt(LocalDateTime.now());
        order.setStatus("rejected");
        orderMapper.updateById(order);

        log.info("管理员拒绝收款 - 订单: {}, 原因: {}", order.getOrderNo(), note);

        // 发送拒绝通知邮件
        try {
            String email = order.getCustomerEmail();
            String siteName = siteSettingService.getSetting("site_name");
            if (siteName == null || siteName.isEmpty()) siteName = "CC-Installer";
            if (email != null && !email.isEmpty()) {
                String template = configService.getConfig("reject_template");
                if (template == null || template.isEmpty()) {
                    template = "<div><h2>订单已被拒绝</h2><p>您的订单 {order_no} 已被管理员拒绝。</p><p>原因: {reason}</p></div>";
                }
                String content = template
                    .replace("{site_name}", siteName)
                    .replace("{order_no}", order.getOrderNo())
                    .replace("{amount}", order.getAmount() != null ? order.getAmount().toString() : "")
                    .replace("{reason}", note)
                    .replace("{email}", email);
                emailService.sendEmail(email, siteName + " - 订单已被拒绝", content);
                log.info("拒绝通知邮件已发送 -> {}", email);
            }
        } catch (Exception e) {
            log.warn("拒绝通知邮件发送失败: {}", e.getMessage());
        }

        return ApiResponse.success();
    }

    /**
     * 模拟支付（仅用于沙箱/测试环境，生产环境请勿使用）
     */
    @PostMapping("/{id}/simulate-pay")
    public ApiResponse<Void> simulatePayment(@PathVariable Long id) {
        log.warn("[TEST ONLY] 模拟支付请求 - 订单ID: {} (此接口仅用于测试环境)", id);

        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        if (!"pending".equals(order.getStatus()) && !"pending_verify".equals(order.getStatus())) {
            return ApiResponse.error("订单状态不正确，当前状态: " + order.getStatus());
        }

        // 1. 标记已支付
        order.setStatus("paid");
        order.setPaymentMethod("wechat");
        order.setPaymentNo("SIM-" + System.currentTimeMillis());
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 2. 计算销售分润
        if (order.getSalesId() != null) {
            commissionService.calculateCommission(order.getId());
        }

        // 3. 自动发货：生成授权码
        autoDeliver(order);

        log.info("订单 {} 已模拟支付并自动发货", order.getOrderNo());
        return ApiResponse.success();
    }

    /**
     * 自动发货：生成授权码（支付完成后自动调用）
     */
    private void autoDeliver(Order order) {
        try {
            // 根据订单的 package_id 获取套餐信息
            ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;

            // 如果订单没有产品ID，从套餐获取
            if (order.getProductId() == null && pkg != null) {
                order.setProductId(pkg.getProductId());
            }

            // 获取授权有效期（优先使用套餐配置）
            int validityHours = pkg != null && pkg.getAuthValidityHours() != null ? pkg.getAuthValidityHours() : 24;

            // 产品名称
            String productName = order.getProductName();
            if (productName == null && pkg != null) {
                productName = pkg.getName();
                order.setProductName(productName);
            }

            // 生成授权码
            String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

            // 保存授权码到订单
            order.setAuthCode(authCode);
            order.setAuthStatus("active");
            order.setStatus("delivered");
            orderMapper.updateById(order);

            // 保存到 license_codes 表（用于核销）
            try {
                authCodeService.saveLicenseCode(order.getId(), authCode);
            } catch (Exception e) {
                log.warn("license_codes 保存失败(不影响发货): {}", e.getMessage());
            }

            // 同步注册到 Cloudflare Worker
            try {
                String codeId = authCodeService.parseCodeId(authCode);
                if (codeId != null) {
                    String json = String.format("{\"code\":\"%s\",\"codeId\":\"%s\",\"version\":\"sale-system\"}",
                            authCode.replace("\"", "\\\""), codeId);
                    URI uri = URI.create("https://auth.wonderhow.store/api/auth/register");
                    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.getBytes("UTF-8"));
                    }
                    int respCode = conn.getResponseCode();
                    log.info("Cloudflare Worker 注册结果: {} - {}", respCode, authCode.substring(0, Math.min(30, authCode.length())) + "...");
                }
            } catch (Exception e) {
                log.warn("Cloudflare Worker 注册失败(不影响发货): {}", e.getMessage());
            }

            // 获取下载链接（优先使用套餐配置的链接）
            String downloadUrl = "";
            if (pkg != null && pkg.getDownloadUrl() != null && !pkg.getDownloadUrl().isEmpty()) {
                downloadUrl = pkg.getDownloadUrl();
            }

            // 构建发货消息
            String template = configService.getConfig("delivery_template");
            if (template == null || template.isEmpty()) {
                template = "感谢您的购买！授权码: {auth_code}";
            }

            String siteName = siteSettingService.getSetting("site_name");

            String message = template
                    .replace("{site_name}", siteName != null ? siteName : "系统")
                    .replace("{order_no}", order.getOrderNo())
                    .replace("{amount}", order.getAmount().toString())
                    .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                    .replace("{download_url}", downloadUrl)
                    .replace("{auth_code}", authCode)
                    .replace("{product_name}", productName != null ? productName : "")
                    .replace("{package_name}", order.getPackageName() != null ? order.getPackageName() : "")
                    .replace("{platform}", order.getPlatform() != null ? order.getPlatform() : "")
                    .replace("{version}", pkg != null && pkg.getVersion() != null ? pkg.getVersion() : "");

            // 发送短信（开发环境仅打印日志）
            log.info("[自动发货] 订单: {}, 手机: {}, 套餐: {}, 授权码: {}",
                    order.getOrderNo(), order.getCustomerPhone(), order.getPackageName(), authCode);
            log.info("[自动发货] 短信内容: {}", message);

            // 发送邮件通知客户
            sendDeliveryEmail(order, downloadUrl, authCode);

        } catch (Exception e) {
            log.error("自动发货失败 - 订单: {}", order.getOrderNo(), e);
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

        // 将相对路径转为绝对URL
        if (downloadUrl != null && downloadUrl.startsWith("/")) {
            downloadUrl = siteUrl + downloadUrl;
        }

        // 使用管理端可配置的 delivery_template
        String template = configService.getConfig("delivery_template");
        if (template == null || template.isEmpty()) {
            template = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #667eea;">{product_name} - 订单已发货</h2>
                    <p>感谢您的购买！</p>
                    <p><strong>订单号：</strong>{order_no}</p>
                    <p><strong>套餐：</strong>{package_name}</p>
                    <p><strong>金额：</strong>¥{amount}</p>
                    <p><strong>授权码：</strong><code style="background: #f3f4f6; padding: 4px 8px; border-radius: 4px;">{auth_code}</code></p>
                    <p>🔐 授权码有效期 {hours} 小时，一机一码，激活后即失效</p>
                    <p>请点击下方链接下载安装器：</p>
                    <a href="{download_url}" style="display: inline-block; padding: 12px 24px; background: #667eea; color: white; text-decoration: none; border-radius: 8px; margin: 16px 0;">📥 下载安装器</a>
                    <p style="color: #999; font-size: 12px; margin-top: 24px;">CC-Installer 自动发货邮件</p>
                </div>
                """;
        }

        // 获取授权有效期
        int validityHours = 24;
        ProductPackage pkg = order.getPackageId() != null ? productPackageMapper.selectById(order.getPackageId()) : null;
        if (pkg != null && pkg.getAuthValidityHours() != null) {
            validityHours = pkg.getAuthValidityHours();
        }

        // 生成带 token 的订单查询链接
        String orderUrl = siteUrl + "/orders?orderNo=" + order.getOrderNo();

        String content = template
                .replace("{site_name}", siteSettingService.getSetting("site_name") != null ? siteSettingService.getSetting("site_name") : "CC-Installer")
                .replace("{site_url}", siteUrl)
                .replace("{order_no}", order.getOrderNo())
                .replace("{amount}", order.getAmount().toString())
                .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                .replace("{email}", email)
                .replace("{download_url}", orderUrl)
                .replace("{order_url}", orderUrl)
                .replace("{auth_code}", authCode != null ? authCode : "")
                .replace("{package_name}", order.getPackageName() != null ? order.getPackageName() : "")
                .replace("{product_name}", order.getProductName() != null ? order.getProductName() : "")
                .replace("{platform}", order.getPlatform() != null ? order.getPlatform() : "")
                .replace("{hours}", String.valueOf(validityHours));

        boolean sent = emailService.sendEmail(email, siteSettingService.getSetting("site_name") + " - 订单已发货", content);
        if (sent) {
            log.info("发货邮件已发送 -> {}", email);
        } else {
            log.warn("发货邮件发送失败 -> {}", email);
        }
    }
}