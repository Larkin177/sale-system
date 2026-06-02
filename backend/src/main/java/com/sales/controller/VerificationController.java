package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/send")
    public ApiResponse<Void> send(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isEmpty()) {
            return ApiResponse.error("手机号不能为空");
        }
        String result = verificationService.sendCode(phone, "phone_verify");
        if ("success".equals(result)) {
            return ApiResponse.success();
        }
        return ApiResponse.error(result);
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verify(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        if (phone == null || phone.isEmpty() || code == null || code.isEmpty()) {
            return ApiResponse.error("手机号和验证码不能为空");
        }
        String result = verificationService.verifyCode(phone, code, "phone_verify");
        if ("success".equals(result)) {
            return ApiResponse.success();
        }
        return ApiResponse.error(result);
    }
}
