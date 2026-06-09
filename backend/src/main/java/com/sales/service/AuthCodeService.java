package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.LicenseCode;
import com.sales.mapper.LicenseCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private final LicenseCodeMapper licenseCodeMapper;
    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("keys/private.pem");
            if (resource.exists()) {
                String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
                byte[] keyBytes = Base64.getDecoder().decode(key);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                privateKey = kf.generatePrivate(spec);
                log.info("Auth code RSA private key loaded");
            } else {
                log.warn("Private key not found, auth code generation disabled");
            }
        } catch (Exception e) {
            log.error("Failed to load private key", e);
        }
    }

    /**
     * Generate an authorization code for an order
     * Format: AIC-{payload_base64}.{signature_base64}
     */
    public String generateAuthCode(Long orderId, int validityHours) {
        if (privateKey == null) {
            log.warn("Private key not loaded, generating mock auth code");
            return "AIC-MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }

        try {
            // Create payload
            String codeId = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            long now = System.currentTimeMillis() / 1000;
            long expiresAt = now + (long) validityHours * 3600;

            String payloadJson = String.format(
                "{\"id\":\"%s\",\"orderId\":%d,\"exp\":%d,\"iat\":%d,\"ver\":1}",
                codeId, orderId, expiresAt, now);

            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Sign
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payloadBase64.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = signature.sign();
            String sigBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);

            return "AIC-" + payloadBase64 + "." + sigBase64;
        } catch (Exception e) {
            log.error("Failed to generate auth code", e);
            return "AIC-ERROR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
    }

    public boolean isAvailable() {
        return privateKey != null;
    }

    /**
     * 从授权码中提取 payload 中的 codeId
     * 格式: AIC-{payload_base64}.{signature_base64}
     */
    public String parseCodeId(String authCode) {
        if (authCode == null || !authCode.startsWith("AIC-")) {
            return null;
        }
        try {
            String payloadBase64 = authCode.substring(4);
            int dotIndex = payloadBase64.indexOf('.');
            if (dotIndex < 0) return null;
            payloadBase64 = payloadBase64.substring(0, dotIndex);

            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
            // Parse JSON to extract "id" field
            // Simple parsing without Jackson dependency in this context
            int idIdx = payloadJson.indexOf("\"id\":\"");
            if (idIdx < 0) return null;
            idIdx += 6;
            int endIdx = payloadJson.indexOf("\"", idIdx);
            return endIdx > idIdx ? payloadJson.substring(idIdx, endIdx) : null;
        } catch (Exception e) {
            log.warn("Failed to parse codeId from authCode", e);
            return null;
        }
    }

    /**
     * 保存授权码到 license_codes 表
     * 在发货时调用
     */
    public void saveLicenseCode(Long orderId, String authCode) {
        if (authCode == null || orderId == null) return;

        // 检查是否已存在（幂等）
        LicenseCode existing = licenseCodeMapper.selectOne(
                new LambdaQueryWrapper<LicenseCode>()
                        .eq(LicenseCode::getCode, authCode));
        if (existing != null) return;

        String codeId = parseCodeId(authCode);
        if (codeId == null) {
            log.warn("无法解析授权码 codeId, 跳过保存: orderId={}, authCode={}", orderId, authCode);
            return;
        }

        LicenseCode lc = new LicenseCode();
        lc.setCode(authCode);
        lc.setCodeId(codeId);
        lc.setOrderId(orderId);
        lc.setConsumed(0);
        licenseCodeMapper.insert(lc);

        log.info("授权码已保存到 license_codes: orderId={}, codeId={}", orderId, codeId);
    }
}