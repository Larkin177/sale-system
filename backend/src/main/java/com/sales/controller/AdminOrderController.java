package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.mapper.OrderMapper;
import com.sales.service.AuthCodeService;
import com.sales.service.CommissionService;
import com.sales.service.ConfigService;
import com.sales.service.OrderService;
import com.sales.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final CommissionService commissionService;
    private final AuthCodeService authCodeService;
    private final ConfigService configService;
    private final SiteSettingService siteSettingService;

    @GetMapping
    public ApiResponse<Page<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.listAllOrders(page, size);
    }

    @GetMapping("/unclaimed")
    public ApiResponse<List<Order>> unclaimed() {
        return orderService.listUnclaimedOrders();
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
        if (!"pending".equals(order.getStatus())) {
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

        // 3. 自动发货：生成授权码 + 发送短信
        autoDeliver(order);

        log.info("订单 {} 已模拟支付并自动发货", order.getOrderNo());
        return ApiResponse.success();
    }

    /**
     * 自动发货：生成授权码 + 发送短信（支付完成后自动调用）
     */
    private void autoDeliver(Order order) {
        try {
            // 生成授权码
            int validityHours = 72;
            String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

            // 保存授权码到订单
            order.setAuthCode(authCode);
            order.setAuthStatus("active");
            order.setProductId(1L); // CC-Installer
            order.setStatus("delivered");
            orderMapper.updateById(order);

            // 构建发货消息
            String template = configService.getConfig("delivery_template");
            if (template == null || template.isEmpty()) {
                template = "感谢您的购买！授权码: {auth_code}";
            }

            String siteName = siteSettingService.getSetting("site_name");
            String downloadUrl = "http://localhost:3000/download?token=auto";

            String message = template
                    .replace("{site_name}", siteName != null ? siteName : "系统")
                    .replace("{order_no}", order.getOrderNo())
                    .replace("{amount}", order.getAmount().toString())
                    .replace("{phone}", order.getCustomerPhone() != null ? order.getCustomerPhone() : "")
                    .replace("{download_url}", downloadUrl)
                    .replace("{auth_code}", authCode);

            // 发送短信（开发环境仅打印日志）
            log.info("[自动发货] 订单: {}, 手机: {}, 授权码: {}",
                    order.getOrderNo(), order.getCustomerPhone(), authCode);
            log.info("[自动发货] 短信内容: {}", message);

        } catch (Exception e) {
            log.error("自动发货失败 - 订单: {}", order.getOrderNo(), e);
        }
    }
}
