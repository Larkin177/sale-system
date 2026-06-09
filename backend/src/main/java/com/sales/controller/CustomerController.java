package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final OrderMapper orderMapper;

    @GetMapping("/api/customer/orders")
    public ApiResponse<List<Map<String, Object>>> getOrders(@RequestParam String email) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCustomerEmail, email)
                        .orderByDesc(Order::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("amount", o.getAmount());
            m.put("status", o.getStatus());
            m.put("authCode", o.getAuthCode());
            m.put("authStatus", o.getAuthStatus());
            m.put("productName", o.getProductName());
            m.put("packageName", o.getPackageName());
            m.put("createdAt", o.getCreatedAt());
            result.add(m);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/api/customer/order")
    public ApiResponse<Map<String, Object>> getOrder(@RequestParam String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("orderNo", order.getOrderNo());
        m.put("amount", order.getAmount());
        m.put("status", order.getStatus());
        m.put("authCode", order.getAuthCode());
        m.put("authStatus", order.getAuthStatus());
        m.put("productName", order.getProductName());
        m.put("packageName", order.getPackageName());
        m.put("createdAt", order.getCreatedAt());
        m.put("customerEmail", order.getCustomerEmail());
        return ApiResponse.success(m);
    }
}
