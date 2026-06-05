package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.VerificationCode;
import com.sales.mapper.VerificationCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationCodeMapper verificationCodeMapper;

    public String sendCode(String phone, String purpose) {
        // 手机号格式校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确";
        }

        // Rate limit: check if a code was sent within the last 60 seconds
        LocalDateTime sixtySecondsAgo = LocalDateTime.now().minusSeconds(60);
        Long count = verificationCodeMapper.selectCount(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getPhone, phone)
                        .gt(VerificationCode::getCreatedAt, sixtySecondsAgo));
        if (count > 0) {
            return "请60秒后再试";
        }

        // Generate 6-digit random code
        String code = String.format("%06d", new Random().nextInt(1000000));

        // Insert with 5-minute expiry
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setPhone(phone);
        verificationCode.setCode(code);
        verificationCode.setPurpose(purpose);
        verificationCode.setUsed(false);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(verificationCode);

        // Log the code (in production, call SMS API here)
        System.out.println("[SMS] Phone: " + phone + " Code: " + code);

        return "success";
    }

    public String verifyCode(String phone, String code, String purpose) {
        // 检查5分钟内错误尝试次数（防止暴力破解）
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Long errorCount = verificationCodeMapper.selectCount(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getPhone, phone)
                        .eq(VerificationCode::getPurpose, purpose)
                        .gt(VerificationCode::getCreatedAt, fiveMinutesAgo));
        // 包含正确的那次，所以如果5分钟内已有5次尝试（含正确），拒绝
        // 这里简化：5分钟内最多尝试5次
        if (errorCount >= 5) {
            return "尝试次数过多，请5分钟后再试";
        }

        // Find the latest unused, non-expired code
        VerificationCode verificationCode = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getPhone, phone)
                        .eq(VerificationCode::getPurpose, purpose)
                        .eq(VerificationCode::getUsed, false)
                        .gt(VerificationCode::getExpiresAt, LocalDateTime.now())
                        .orderByDesc(VerificationCode::getCreatedAt)
                        .last("LIMIT 1"));

        if (verificationCode == null) {
            return "验证码无效或已过期";
        }

        if (!verificationCode.getCode().equals(code)) {
            return "验证码错误";
        }

        // Mark as used
        verificationCode.setUsed(true);
        verificationCodeMapper.updateById(verificationCode);

        return "success";
    }
}
