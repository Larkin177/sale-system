package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.dto.CreateOrderRequest;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.service.AlipayService;
import com.sales.service.OrderService;
import com.sales.service.SalesService;
import com.sales.service.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;
    private final SalesService salesService;
    private final SiteSettingService siteSettingService;
    private final AlipayService alipayService;

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        // 根据销售码获取销售ID
        Long salesId = null;
        if (request.getSalesCode() != null && !request.getSalesCode().isEmpty()) {
            Sales sales = salesService.getByCode(request.getSalesCode());
            if (sales != null) {
                salesId = sales.getId();
            }
        }

        // 创建订单
        Order order = orderService.createOrder(salesId, request.getAmount());

        // 保存客户价格记忆
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            orderService.saveCustomerPrice(request.getPhone(), request.getAmount(), salesId, order.getOrderNo());
        }

        // 获取支付二维码
        String wechatQrcode = siteSettingService.getSetting("wechat_qrcode");
        String alipayQrcode = siteSettingService.getSetting("alipay_qrcode");

        // 创建支付宝当面付预下单，获取收款二维码
        String alipayQrCode = "";
        Map<String, String> alipayResult = alipayService.createQrCode(
                order.getOrderNo(),
                order.getAmount().toString(),
                "CC-Installer - " + order.getOrderNo()
        );
        if (alipayResult != null) {
            alipayQrCode = alipayResult.getOrDefault("qrCode", "");
        }

        // 返回订单信息
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getAmount());
        result.put("message", "订单创建成功，请完成支付");
        result.put("wechatQrcode", wechatQrcode != null ? wechatQrcode : "");
        result.put("alipayQrcode", alipayQrcode != null ? alipayQrcode : "");
        result.put("alipayQrCode", alipayQrCode);

        return ApiResponse.success(result);
    }
}
