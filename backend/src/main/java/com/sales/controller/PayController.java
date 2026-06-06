package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.dto.CreateOrderRequest;
import com.sales.entity.Order;
import com.sales.entity.Product;
import com.sales.entity.ProductPackage;
import com.sales.entity.Sales;
import com.sales.entity.SystemConfig;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductPackageMapper;
import com.sales.mapper.SystemConfigMapper;
import com.sales.service.AlipayService;
import com.sales.service.OrderService;
import com.sales.service.ProductService;
import com.sales.service.SalesService;
import com.sales.service.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;
    private final SalesService salesService;
    private final ProductService productService;
    private final SiteSettingService siteSettingService;
    private final AlipayService alipayService;
    private final OrderMapper orderMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ProductPackageMapper productPackageMapper;

    /**
     * 获取当前支付模式配置
     */
    @GetMapping("/mode")
    public ApiResponse<Map<String, String>> getPayMode() {
        Map<String, String> modes = new HashMap<>();
        modes.put("wechat", getPayMode("wechat_pay_mode"));
        modes.put("alipay", getPayMode("alipay_pay_mode"));
        return ApiResponse.success(modes);
    }

    private String getPayMode(String configKey) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, configKey));
        if (config == null) {
            // Alipay: if configured, default to api; otherwise static
            if ("alipay_pay_mode".equals(configKey)) {
                return alipayService.isConfigured() ? "api" : "static";
            }
            // Wechat: default to static
            return "static";
        }
        return config.getConfigValue();
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        // 根据 packageId 获取套餐信息和价格
        ProductPackage pkg = productPackageMapper.selectById(request.getPackageId());
        if (pkg == null || !"active".equals(pkg.getStatus())) {
            return ApiResponse.error("套餐不存在或已下架");
        }

        BigDecimal amount = pkg.getPrice();

        // 根据销售码获取销售ID
        Long salesId = null;
        if (request.getSalesCode() != null && !request.getSalesCode().isEmpty()) {
            Sales sales = salesService.getByCode(request.getSalesCode());
            if (sales != null) {
                salesId = sales.getId();
            }
        }

        // 创建订单（带套餐信息）
        Order order = orderService.createOrderWithPackage(salesId, amount, pkg);

        // 保存客户价格记忆
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            orderService.saveCustomerPrice(request.getPhone(), amount, salesId, order.getOrderNo());
        }

        // 获取静态支付二维码
        String wechatQrcode = siteSettingService.getSetting("wechat_qrcode");
        String alipayQrcode = siteSettingService.getSetting("alipay_qrcode");

        // 支付宝API模式：创建当面付预下单，获取收款二维码
        String alipayQrCode = "";
        String alipayMode = getPayMode("alipay_pay_mode");
        if ("api".equals(alipayMode)) {
            Map<String, String> alipayResult = alipayService.createQrCode(
                    order.getOrderNo(),
                    amount.toString(),
                    pkg.getName() + " - " + order.getOrderNo()
            );
            if (alipayResult != null) {
                alipayQrCode = alipayResult.getOrDefault("qrCode", "");
            }
        }

        // 返回订单信息 + 支付模式
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", amount);
        result.put("packageName", pkg.getName());
        result.put("platform", pkg.getPlatform());
        result.put("message", "订单创建成功，请完成支付");
        result.put("wechatQrcode", wechatQrcode != null ? wechatQrcode : "");
        result.put("alipayQrcode", alipayQrcode != null ? alipayQrcode : "");
        result.put("alipayQrCode", alipayQrCode);
        result.put("wechatMode", getPayMode("wechat_pay_mode"));
        result.put("alipayMode", alipayMode);

        return ApiResponse.success(result);
    }

    /**
     * 静态支付模式：客户标记"我已支付"，等待管理员审核
     */
    @PostMapping("/mark-paid")
    public ApiResponse<Map<String, String>> markPaid(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        if (orderNo == null || orderNo.isEmpty()) {
            return ApiResponse.error("订单号不能为空");
        }

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            return ApiResponse.error("订单状态不正确，当前: " + order.getStatus());
        }

        // 静态模式下客户标记已支付，等待管理员审核
        order.setStatus("pending_verify");
        orderMapper.updateById(order);

        log.info("客户标记已支付（静态模式）- 订单: {}", orderNo);

        Map<String, String> result = new HashMap<>();
        result.put("status", "pending_verify");
        result.put("message", "已提交，等待管理员确认收款后自动发货");
        return ApiResponse.success(result);
    }
}
