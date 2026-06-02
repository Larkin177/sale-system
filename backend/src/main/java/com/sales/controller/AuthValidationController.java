package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.AuthValidateRequest;
import com.sales.dto.AuthValidateResponse;
import com.sales.entity.AuthLog;
import com.sales.entity.Order;
import com.sales.mapper.OrderMapper;
import com.sales.service.AuthValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthValidationController {

    private final AuthValidationService authValidationService;
    private final OrderMapper orderMapper;

    @PostMapping("/api/auth/validate")
    public AuthValidateResponse validate(@Valid @RequestBody AuthValidateRequest request,
                                         HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        return authValidationService.validateCode(request.getAuthCode(), request.getMachine(), ipAddress);
    }

    @PostMapping("/api/auth/revoke/{orderId}")
    public ApiResponse<Void> revoke(@PathVariable Long orderId) {
        return authValidationService.revokeCode(orderId);
    }

    /**
     * 客户端核销回调 - 客户端验证授权码成功后调用
     * 更新订单状态为 "redeemed"（已核销）
     */
    @PostMapping("/api/auth/redeem")
    public ApiResponse<Void> redeem(@RequestBody Map<String, String> body) {
        String authCode = body.get("authCode");
        String machine = body.get("machine");

        if (authCode == null || authCode.isEmpty()) {
            return ApiResponse.error("授权码不能为空");
        }

        // Find order by auth_code
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getAuthCode, authCode));

        if (order == null) {
            return ApiResponse.error("授权码不存在");
        }

        // Update order status to redeemed
        if (!"redeemed".equals(order.getAuthStatus())) {
            order.setAuthStatus("redeemed");
            order.setAuthUsedAt(LocalDateTime.now());
            order.setAuthMachine(machine);
            orderMapper.updateById(order);
            log.info("Auth code redeemed: order={}, machine={}", order.getOrderNo(), machine);
        }

        return ApiResponse.success();
    }

    @GetMapping("/api/admin/auth/logs")
    public ApiResponse<Page<AuthLog>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return authValidationService.listAuthLogs(page, size);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For may contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
