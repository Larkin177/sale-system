package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.AuthValidateResponse;
import com.sales.entity.AuthLog;
import com.sales.entity.Order;
import com.sales.entity.Product;
import com.sales.mapper.AuthLogMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthValidationService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final AuthLogMapper authLogMapper;

    @Transactional
    public AuthValidateResponse validateCode(String authCode, String machine, String ipAddress) {
        // 1. 根据auth_code查找订单（取最新的一条）
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getAuthCode, authCode)
                        .orderByDesc(Order::getId).last("LIMIT 1"));
        if (order == null) {
            logAuth(null, authCode, "validate", machine, ipAddress, "failed", "授权码不存在");
            return buildError("授权码不存在");
        }

        // 2. 检查auth_status
        if ("revoked".equals(order.getAuthStatus())) {
            logAuth(order.getId(), authCode, "validate", machine, ipAddress, "failed", "授权码已被撤销");
            return buildError("授权码已被撤销");
        }
        if ("expired".equals(order.getAuthStatus())) {
            logAuth(order.getId(), authCode, "validate", machine, ipAddress, "failed", "授权码已过期");
            return buildError("授权码已过期");
        }

        // 3. 检查产品是否启用授权验证
        if (order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null && !Boolean.TRUE.equals(product.getAuthEnabled())) {
                logAuth(order.getId(), authCode, "validate", machine, ipAddress, "failed", "该产品未启用授权验证");
                return buildError("该产品未启用授权验证");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 4. 首次使用：绑定机器、设置验证时间
        if (order.getAuthUses() == null || order.getAuthUses() == 0) {
            order.setAuthMachine(machine);
            order.setAuthUsedAt(now);
            order.setAuthStatus("used");
            order.setAuthUses(1);
            order.setAuthLastValidatedAt(now);
            orderMapper.updateById(order);

            logAuth(order.getId(), authCode, "validate", machine, ipAddress, "success", null);
            return buildSuccess(order);
        }

        // 5. 后续使用：比较机器指纹
        if (!machine.equals(order.getAuthMachine())) {
            logAuth(order.getId(), authCode, "validate", machine, ipAddress, "failed", "机器指纹不匹配");
            return buildError("机器指纹不匹配");
        }

        // 6. 检查授权是否过期
        if (order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null && product.getAuthValidityHours() != null && order.getAuthUsedAt() != null) {
                LocalDateTime expiresAt = order.getAuthUsedAt().plusHours(product.getAuthValidityHours());
                if (now.isAfter(expiresAt)) {
                    order.setAuthStatus("expired");
                    orderMapper.updateById(order);

                    logAuth(order.getId(), authCode, "validate", machine, ipAddress, "failed", "授权已过期");
                    return buildError("授权已过期");
                }
            }
        }

        // 7. 更新验证次数
        order.setAuthUses(order.getAuthUses() + 1);
        order.setAuthLastValidatedAt(now);
        orderMapper.updateById(order);

        logAuth(order.getId(), authCode, "validate", machine, ipAddress, "success", null);
        return buildSuccess(order);
    }

    @Transactional
    public ApiResponse<Void> revokeCode(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        order.setAuthStatus("revoked");
        orderMapper.updateById(order);
        return ApiResponse.success();
    }

    public ApiResponse<AuthValidateResponse> getOrderAuthInfo(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        AuthValidateResponse response = new AuthValidateResponse();
        response.setOk(true);
        response.setAuthStatus(order.getAuthStatus());
        response.setUses(order.getAuthUses());

        // 计算过期时间
        if (order.getProductId() != null && order.getAuthUsedAt() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null && product.getAuthValidityHours() != null) {
                response.setExpiresAt(order.getAuthUsedAt().plusHours(product.getAuthValidityHours()));
            }
        }

        return ApiResponse.success(response);
    }

    public ApiResponse<Page<AuthLog>> listAuthLogs(int page, int size) {
        Page<AuthLog> pageParam = new Page<>(page, size);
        Page<AuthLog> result = authLogMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AuthLog>().orderByDesc(AuthLog::getCreatedAt));
        return ApiResponse.success(result);
    }

    private void logAuth(Long orderId, String authCode, String action, String machine, String ipAddress, String result, String reason) {
        AuthLog log = new AuthLog();
        log.setOrderId(orderId);
        log.setAuthCode(authCode);
        log.setAction(action);
        log.setMachine(machine);
        log.setIpAddress(ipAddress);
        log.setResult(result);
        log.setReason(reason);
        authLogMapper.insert(log);
    }

    private AuthValidateResponse buildSuccess(Order order) {
        AuthValidateResponse response = new AuthValidateResponse();
        response.setOk(true);
        response.setUses(order.getAuthUses());
        response.setAuthStatus(order.getAuthStatus());

        if (order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                response.setProduct(product.getName());
                if (product.getAuthValidityHours() != null && order.getAuthUsedAt() != null) {
                    response.setExpiresAt(order.getAuthUsedAt().plusHours(product.getAuthValidityHours()));
                }
            }
        }

        return response;
    }

    private AuthValidateResponse buildError(String message) {
        AuthValidateResponse response = new AuthValidateResponse();
        response.setOk(false);
        response.setError(message);
        return response;
    }
}
