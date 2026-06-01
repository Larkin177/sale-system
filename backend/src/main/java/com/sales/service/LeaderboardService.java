package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final SalesMapper salesMapper;
    private final OrderMapper orderMapper;

    public ApiResponse<List<Map<String, Object>>> getTop3() {
        // 直接从数据库查询本月销售排行榜
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // 查询所有已支付的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "paid")
                        .ge(Order::getPaidAt, monthStart));

        // 按销售ID分组统计
        Map<Long, Double> salesAmountMap = new HashMap<>();
        for (Order order : orders) {
            if (order.getSalesId() != null) {
                salesAmountMap.merge(order.getSalesId(), order.getAmount().doubleValue(), Double::sum);
            }
        }

        // 排序并取前3
        List<Map<String, Object>> result = new ArrayList<>();
        salesAmountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    Sales sales = salesMapper.selectById(entry.getKey());
                    if (sales != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("rank", result.size() + 1);
                        item.put("name", maskName(sales.getName()));
                        item.put("amount", entry.getValue());
                        result.add(item);
                    }
                });

        return ApiResponse.success(result);
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
