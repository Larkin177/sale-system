package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final OrderMapper orderMapper;
    private final SalesMapper salesMapper;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        // 总营收
        List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "paid"));
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总订单数
        long totalOrders = allOrders.size();

        // 活跃销售数
        long activeSales = salesMapper.selectCount(
                new LambdaQueryWrapper<Sales>().eq(Sales::getStatus, "active"));

        // 本月数据
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<Order> monthOrders = allOrders.stream()
                .filter(o -> o.getPaidAt() != null && o.getPaidAt().isAfter(monthStart))
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalCommission", totalRevenue.multiply(new BigDecimal("0.1"))); // 假设10%分润
        stats.put("totalOrders", totalOrders);
        stats.put("activeSales", activeSales);
        stats.put("monthOrders", monthOrders.size());
        stats.put("monthRevenue", monthOrders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return ApiResponse.success(stats);
    }
}
