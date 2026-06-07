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

            // 生成授权码
            String authCode = authCodeService.generateAuthCode(order.getId(), validityHours);

            // 更新订单的授权信息
            order.setAuthCode(authCode);
            order.setAuthStatus("active");
            order.setStatus("delivered");
            orderMapper.updateById(order);

            log.info("自动发货成功，订单号: {}, 授权码: {}", order.getOrderNo(), authCode);
        } catch (Exception e) {
            log.error("自动发货失败，订单号: {}", order.getOrderNo(), e);
        }
    }

    private void sendDeliveryEmail(Order order) {
        String email = order.getCustomerEmail();
        if (email == null || email.isEmpty()) return;

        String siteUrl = "http://localhost:3000";
        SystemConfig sc = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "site_url"));
        if (sc != null && sc.getConfigValue() != null && !sc.getConfigValue().isEmpty()) {
            siteUrl = sc.getConfigValue();
        }

        String content = "Order " + order.getOrderNo() + " is ready. Visit " + siteUrl + " to get your auth code.";
        if (order.getPackageId() != null) {
            ProductPackage pp = productPackageMapper.selectById(order.getPackageId());
            if (pp != null && pp.getEmailTemplate() != null && !pp.getEmailTemplate().isEmpty()) {
                content = pp.getEmailTemplate();
                content = content.replace("SITE_URL", siteUrl);
                content = content.replace("ORDER_NO", order.getOrderNo());
                content = content.replace("ORDER_AMOUNT", order.getAmount().toString());
            }
        }

        emailService.sendEmail(email, "CC-Installer: Your order is ready!", content);
    }
}