package com.sales.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.dto.ApiResponse;
import com.sales.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或token无效");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            String role = jwtUtil.getRole(token);
            String userId = String.valueOf(jwtUtil.getUserId(token));

            // 管理端接口只能 admin 角色访问
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/admin") && !"admin".equals(role)) {
                writeError(response, 403, "权限不足");
                return false;
            }

            // 销售端接口只能 sales 角色访问
            if (uri.startsWith("/api/orders") && !"sales".equals(role)) {
                writeError(response, 403, "权限不足");
                return false;
            }

            // 将用户信息存入 request 供后续使用
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
            return true;

        } catch (Exception e) {
            writeError(response, 401, "token无效或已过期");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
