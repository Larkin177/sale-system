package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Sales;
import com.sales.mapper.SalesMapper;
import com.sales.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesAuthController {

    private final SalesMapper salesMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) return ApiResponse.error("请输入邮箱");

        Sales sales = salesMapper.selectOne(new LambdaQueryWrapper<Sales>().eq(Sales::getEmail, email));
        if (sales == null) return ApiResponse.error("该邮箱未注册");

        String newPassword = java.util.UUID.randomUUID().toString().substring(0, 10);
        sales.setPassword(passwordEncoder.encode(newPassword));
        salesMapper.updateById(sales);

        try {
            emailService.sendEmail(email, "CC-Installer - 密码已重置",
                "<h2>密码重置成功</h2><p>您的新密码: <strong>" + newPassword + "</strong></p><p>请登录后立即修改密码。</p>");
            log.info("密码重置邮件已发送 -> {}", email);
        } catch (Exception e) {
            log.warn("密码重置邮件发送失败: {}", e.getMessage());
        }

        return ApiResponse.success();
    }
}
