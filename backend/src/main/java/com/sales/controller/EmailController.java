package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * 发送自定义邮件（管理员手动发送）
     */
    @PostMapping("/send")
    public ApiResponse<Void> sendEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        String subject = body.get("subject");
        String content = body.get("content");

        if (to == null || to.trim().isEmpty()) {
            return ApiResponse.error("收件人邮箱不能为空");
        }
        if (subject == null || subject.trim().isEmpty()) {
            return ApiResponse.error("邮件主题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error("邮件内容不能为空");
        }

        boolean ok = emailService.sendEmail(to.trim(), subject.trim(), content);
        if (ok) {
            log.info("管理员手动发送邮件成功 -> {}", to);
            return ApiResponse.success();
        } else {
            return ApiResponse.error("邮件发送失败，请检查邮件服务器配置");
        }
    }
}
