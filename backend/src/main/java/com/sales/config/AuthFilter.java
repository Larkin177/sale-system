package com.sales.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.dto.ApiResponse;
import com.sales.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // 公开接口前缀
    private static final String[] PUBLIC_PREFIXES = {
        "/api/auth/validate",
        "/api/auth/redeem",
        "/api/auth/sales/login",
        "/api/auth/admin/login",
        "/api/sales/register",
        "/api/config",
        "/api/pay/",
        "/api/payment/",
        "/api/download/",
        "/api/leaderboard/",
        "/api/site-settings",
        "/api/customer/",
        "/api/verification/",
        "/api/captcha/",
        "/api/alipay/",
        "/api/webhook/",
        "/api/products/"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // OPTIONS 放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String uri = request.getRequestURI();
        try { java.nio.file.Files.write(java.nio.file.Paths.get("C:/temp/filter-debug.log"),
                ("FILTER URI=" + uri + " METHOD=" + request.getMethod() + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}

        // 公开接口放行
        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.startsWith(prefix)) {
                chain.doFilter(req, res);
                return;
            }
        }

        // 需要认证
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或token无效");
            return;
        }

        String token = authHeader.substring(7);
        try {
            String role = jwtUtil.getRole(token);
            String userId = String.valueOf(jwtUtil.getUserId(token));

            // 管理端接口只能 admin 角色访问
            if (uri.startsWith("/api/admin") && !"admin".equals(role)) {
                writeError(response, 403, "权限不足");
                return;
            }

            // 销售端接口只能 sales 角色访问
            if (uri.startsWith("/api/orders") && !"sales".equals(role)) {
                writeError(response, 403, "权限不足");
                return;
            }

            // 将用户信息存入 request
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
            chain.doFilter(req, res);

        } catch (Exception e) {
            writeError(response, 401, "token无效或已过期");
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
