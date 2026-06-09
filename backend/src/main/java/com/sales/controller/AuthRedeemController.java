package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.LicenseCode;
import com.sales.entity.Order;
import com.sales.mapper.LicenseCodeMapper;
import com.sales.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRedeemController {

    private final LicenseCodeMapper licenseCodeMapper;
    private final OrderMapper orderMapper;

    /**
     * 查询授权码是否已核销
     * 安装器调用：检查授权码是否可用
     */
    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> check(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return ApiResponse.error("授权码不能为空");
        }

        LicenseCode licenseCode = licenseCodeMapper.selectOne(
                new LambdaQueryWrapper<LicenseCode>()
                        .eq(LicenseCode::getCode, code));

        if (licenseCode == null) {
            // 授权码不存在 -> 按已核销处理，防止未授权使用
            log.warn("授权码不存在: {}", code);
            return ApiResponse.success(Map.of("consumed", true));
        }

        boolean consumed = licenseCode.getConsumed() != null && licenseCode.getConsumed() == 1;
        log.info("授权码查询: code={}, consumed={}", code, consumed);

        return ApiResponse.success(Map.of("consumed", consumed));
    }

    /**
     * 核销授权码
     * 安装器调用：首次验证通过后标记为已使用
     * 幂等：重复调用返回成功
     */
    @PostMapping("/consume")
    public ApiResponse<Map<String, Object>> consume(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String machine = body.get("machine");

        if (code == null || code.isEmpty()) {
            return ApiResponse.error("授权码不能为空");
        }

        LicenseCode licenseCode = licenseCodeMapper.selectOne(
                new LambdaQueryWrapper<LicenseCode>()
                        .eq(LicenseCode::getCode, code));

        if (licenseCode == null) {
            log.warn("核销失败，授权码不存在: {}", code);
            return ApiResponse.success(Map.of("success", true, "message", "授权码不存在，已忽略"));
        }

        // 已核销 -> 幂等返回成功
        if (licenseCode.getConsumed() != null && licenseCode.getConsumed() == 1) {
            log.info("授权码已被核销(幂等处理): code={}", code);
            return ApiResponse.success(Map.of("success", true));
        }

        // 标记核销
        licenseCode.setConsumed(1);
        licenseCode.setConsumedAt(LocalDateTime.now());
        licenseCode.setConsumedBy(machine != null ? machine : "unknown");
        licenseCodeMapper.updateById(licenseCode);

        // 同步更新订单的 auth_status
        Order order = orderMapper.selectById(licenseCode.getOrderId());
        if (order != null) {
            order.setAuthStatus("consumed");
            order.setAuthUsedAt(LocalDateTime.now());
            order.setAuthMachine(machine);
            orderMapper.updateById(order);
            log.info("订单授权码已核销: orderNo={}, code={}, machine={}",
                    order.getOrderNo(), code, machine);
        }

        log.info("授权码核销成功: code={}, machine={}", code, machine);
        return ApiResponse.success(Map.of("success", true));
    }
}
