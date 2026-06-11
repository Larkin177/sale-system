package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.dto.CreateOrderRequest;
import com.sales.entity.Order;
import com.sales.entity.ProductPackage;
import com.sales.entity.Sales;
import com.sales.entity.SystemConfig;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductPackageMapper;
import com.sales.mapper.SystemConfigMapper;
import com.sales.service.AlipayService;
import com.sales.service.OrderService;
import com.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;
    private final SalesService salesService;
    private final AlipayService alipayService;
    private final OrderMapper orderMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ProductPackageMapper productPackageMapper;

    @GetMapping("/mode")
    public ApiResponse<Map<String, String>> getPayMode() {
        Map<String, String> m = new HashMap<>();
        m.put("wechat", getPayMode("wechat_pay_mode"));
        m.put("alipay", getPayMode("alipay_pay_mode"));
        return ApiResponse.success(m);
    }

    private String cfg(String key) {
        SystemConfig c = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        return c != null ? c.getConfigValue() : "";
    }

    private String getPayMode(String key) {
        SystemConfig c = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (c == null) {
            if ("alipay_pay_mode".equals(key)) return alipayService.isConfigured() ? "api" : "static";
            return "static";
        }
        return c.getConfigValue();
    }

    
    @GetMapping("/packages")
    public ApiResponse<List<ProductPackage>> listPackages() {
        List<ProductPackage> list = productPackageMapper.selectList(
                new LambdaQueryWrapper<ProductPackage>().eq(ProductPackage::getStatus, "active"));
        return ApiResponse.success(list);
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        ProductPackage pkg = productPackageMapper.selectById(req.getPackageId());
        if (pkg == null || !"active".equals(pkg.getStatus())) return ApiResponse.error("invalid package");
        BigDecimal amount = pkg.getPrice();

        Long salesId = null;
        if (req.getSalesCode() != null && !req.getSalesCode().isEmpty()) {
            Sales s = salesService.getByCode(req.getSalesCode());
            if (s != null) salesId = s.getId();
        }

        Order order = orderService.createOrderWithPackage(salesId, amount, pkg);
        if (req.getEmail() != null && !req.getEmail().isEmpty()) {
            order.setCustomerEmail(req.getEmail());
            orderMapper.updateById(order);
        }

        String wechatQr = cfg("wechat_qrcode");
        String alipayQr = cfg("alipay_qrcode");
        String alipayDynQr = "";
        if ("api".equals(getPayMode("alipay_pay_mode"))) {
            Map<String, String> r = alipayService.createQrCode(order.getOrderNo(), amount.toString(), pkg.getName());
            if (r != null) alipayDynQr = r.getOrDefault("qrCode", "");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", amount);
        result.put("packageName", pkg.getName());
        result.put("platform", pkg.getPlatform());
        result.put("wechatQrcode", wechatQr);
        result.put("alipayQrcode", alipayQr);
        result.put("alipayQrCode", alipayDynQr);
        result.put("wechatMode", getPayMode("wechat_pay_mode"));
        result.put("alipayMode", getPayMode("alipay_pay_mode"));
        return ApiResponse.success(result);
    }

    @PostMapping("/mark-paid")
    public ApiResponse<Map<String, String>> markPaid(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        if (orderNo == null || orderNo.isEmpty()) return ApiResponse.error("orderNo required");
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) return ApiResponse.error("not found");
        if (!"pending".equals(order.getStatus())) return ApiResponse.error("status: " + order.getStatus());
        String method = body.get("paymentMethod");
        if (method != null && !method.isEmpty()) {
            order.setPaymentMethod(method);
        }
        order.setStatus("pending_verify");
        orderMapper.updateById(order);
        return ApiResponse.success(Map.of("status", "pending_verify", "message", "submitted"));
    }
}
