package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.dto.CreateOrderRequest;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.service.OrderService;
import com.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;
    private final SalesService salesService;

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        // 根据销售码获取销售ID
        Long salesId = null;
        if (request.getSalesCode() != null && !request.getSalesCode().isEmpty()) {
            Sales sales = salesService.getByCode(request.getSalesCode());
            if (sales != null) {
                salesId = sales.getId();
            }
        }

        // 创建订单
        Order order = orderService.createOrder(salesId, request.getAmount());

        // 返回订单信息（实际项目中这里应该调用支付接口）
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getAmount());
        result.put("message", "订单创建成功，请完成支付");

        return ApiResponse.success(result);
    }
}
