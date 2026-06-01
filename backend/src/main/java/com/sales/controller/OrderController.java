package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ClaimOrderRequest;
import com.sales.entity.Order;
import com.sales.service.OrderService;
import com.sales.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    @GetMapping("/my")
    public ApiResponse<Page<Order>> myOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long salesId = getSalesIdFromRequest(request);
        return orderService.listMyOrders(salesId, page, size);
    }

    @PostMapping("/claim")
    public ApiResponse<Void> claimOrder(
            HttpServletRequest request,
            @Valid @RequestBody ClaimOrderRequest claimRequest) {
        Long salesId = getSalesIdFromRequest(request);
        return orderService.claimOrder(salesId, claimRequest);
    }

    @GetMapping("/unclaimed")
    public ApiResponse<List<Order>> unclaimedOrders() {
        return orderService.listUnclaimedOrders();
    }

    @GetMapping("/stats")
    public ApiResponse<Object> myStats(HttpServletRequest request) {
        Long salesId = getSalesIdFromRequest(request);
        return orderService.getMyStats(salesId);
    }

    private Long getSalesIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserId(token);
        }
        // 尝试从cookie获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sales_id".equals(cookie.getName())) {
                    return Long.parseLong(cookie.getValue());
                }
            }
        }
        return null;
    }
}
