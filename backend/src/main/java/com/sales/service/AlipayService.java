package com.sales.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AlipayService {

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.gateway-url:}")
    private String gatewayUrl;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @Value("${alipay.return-url:}")
    private String returnUrl;

    @Value("${alipay.sign-type:RSA2}")
    private String signType;

    @Value("${alipay.sandbox:true}")
    private boolean sandbox;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        if (appId != null && !appId.isEmpty() && !"sandbox".equals(appId)) {
            alipayClient = new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, "json", "UTF-8", alipayPublicKey, signType);
            log.info("Alipay client initialized, sandbox={}", sandbox);
        } else {
            log.warn("Alipay not configured, payment will use mock mode");
        }
    }

    /**
     * Create a pre-create order (returns QR code URL)
     * @param orderNo order number
     * @param amount payment amount
     * @param subject order description
     * @return map with qrCode URL, or null if failed
     */
    public Map<String, String> createQrCode(String orderNo, String amount, String subject) {
        if (alipayClient == null) {
            // Mock mode for development
            log.info("[MOCK Alipay] orderNo={}, amount={}, subject={}", orderNo, amount, subject);
            Map<String, String> mockResult = new HashMap<>();
            mockResult.put("qrCode", "https://sandbox.alipay.com/mock/qr/" + orderNo);
            mockResult.put("outTradeNo", orderNo);
            return mockResult;
        }

        try {
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl);
            request.setReturnUrl(returnUrl);

            // Set biz content
            request.setBizContent(String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"}",
                orderNo, amount, subject));

            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            if ("10000".equals(response.getCode())) {
                Map<String, String> result = new HashMap<>();
                result.put("qrCode", response.getQrCode());
                result.put("outTradeNo", response.getOutTradeNo());
                return result;
            } else {
                log.error("Alipay precreate failed: {} - {}", response.getCode(), response.getMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            log.error("Alipay API error", e);
            return null;
        }
    }

    /**
     * Verify callback signature
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (alipayClient == null) {
            return true; // Mock mode always succeeds
        }
        try {
            return AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", signType);
        } catch (AlipayApiException e) {
            log.error("Alipay verify failed", e);
            return false;
        }
    }

    public boolean isConfigured() {
        return alipayClient != null;
    }
}
