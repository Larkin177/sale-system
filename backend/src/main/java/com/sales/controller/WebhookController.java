package com.sales.controller;

import com.sales.entity.Order;
import com.sales.service.CommissionService;
import com.sales.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final OrderService orderService;
    private final CommissionService commissionService;

    @PostMapping("/wechat")
    public String wechatNotify(@RequestBody Map<String, Object> params) {
        log.info("微信支付回调: {}", params);
        // TODO: 验证签名
        String orderNo = (String) params.get("out_trade_no");
        // 更新订单状态
        // 计算分润
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    @PostMapping("/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝回调: {}", params);
        // TODO: 验证签名
        String orderNo = params.get("out_trade_no");
        // 更新订单状态
        // 计算分润
        return "success";
    }
}
