package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.entity.Product;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductMapper;
import com.sales.service.AlipayService;
import com.sales.service.AuthCodeService;
import com.sales.service.CommissionService;
import com.sales.service.ConfigService;
import com.sales.service.OrderService;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final CommissionService commissionService;
    private final AuthCodeService authCodeService;
    private final ConfigService configService;
    private final SiteSettingService siteSettingService;

    /**
     * Create Alipay QR code for an order
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, String>> createPayment(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        if (orderNo == null || orderNo.isEmpty()) {
            return ApiResponse.error("订单号不能为空");
        }

        Order order = orderMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        Map<String, String> result = alipayService.createQrCode(
            orderNo,
            order.getAmount().toString(),
            "CC-Installer - " + orderNo
        );

        if (result != null) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error("支付创建失败");
        }
    }

    /**
     * Alipay async notify callback
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            params.put(entry.getKey(), String.join(",", entry.getValue()));
        }

        log.info("Alipay notify received: {}", params);

        // Verify signature
        if (!alipayService.verifyNotify(params)) {
            log.warn("Alipay notify signature verification failed");
            return "fail";
        }

        // Check payment status
        String tradeStatus = params.get("trade_status");
        String orderNo = params.get("out_trade_no");

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // Update order status
            Order order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            if (order != null && "pending".equals(order.getStatus())) {
                order.setStatus("paid");
                order.setPaymentNo(params.get("trade_no"));
                order.setPaymentMethod("alipay");
                order.setPaidAt(java.time.LocalDateTime.now());
                orderMapper.updateById(order);

                // Calculate commission
                if (order.getSalesId() != null) {
                    commissionService.calculateCommission(order.getId());
                }

                // Auto deliver: generate auth code + send SMS
                autoDeliver(order);

                log.info("Order {} paid via Alipay, auto-delivered", orderNo);
            }
        }

        return "success";
    }

    /**
     * Check if Alipay is configured
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", alipayService.isConfigured());
        status.put("sandbox", true);
        return ApiResponse.success(status);
    }

    /**
     * 自动发货：生成授权码 + 发送短信
     */
    private void autoDeliver(Order order) {
        try {
            // 如果订单没有产品ID，使用默认产品
            if (order.getProductId() == null) {
                order.setProductId(1L);
            }

            // 获取产品配置
            int validityHours = 24;
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                if (product.getAuthValidityHours() != null) {
                    validityHours = product.getAuthValidityHours();
                }
                order.setProductName(product.getName());
            }

            String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

            order.setAuthCode(authCode);
            order.setAuthStatus("active");
            order.setStatus("delivered");
            orderMapper.updateById(order);

            String template = configService.getConfig("delivery_template");
            if (template == null || template.isEmpty()) {
                template = "感谢您的购买！授权码: {auth_code}";
            }

            String siteName = siteSettingService.getSetting("site_name");
            String downloadUrl = siteSettingService.getSetting("site_url");
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                downloadUrl = "http://localhost:3000";
            }

            String message = template
                    .replace("{site_name}", siteName != null ? siteName : "系统")
                    .replace("{order_no}", order.getOrderNo())
                    .replace("{amount}", order.getAmount().toString())
                    .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                    .replace("{download_url}", downloadUrl)
                    .replace("{auth_code}", authCode);

            log.info("[自动发货] 订单: {}, 手机: {}, 授权码: {}",
                    order.getOrderNo(), order.getCustomerPhone(), authCode);
            log.info("[自动发货] 短信内容: {}", message);
        } catch (Exception e) {
            log.error("自动发货失败 - 订单: {}", order.getOrderNo(), e);
        }
    }
}
