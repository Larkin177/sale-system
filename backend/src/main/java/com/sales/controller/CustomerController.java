package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final OrderService orderService;

    @GetMapping("/api/customer/last-order")
    public ApiResponse<Map<String, Object>> getLastOrder(@RequestParam String phone) {
        Map<String, Object> lastOrder = orderService.getCustomerLastOrder(phone);
        if (lastOrder == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("amount", null);
            return ApiResponse.success(empty);
        }
        return ApiResponse.success(lastOrder);
    }

    @GetMapping("/api/customer/price")
    public ApiResponse<Map<String, Object>> getCustomerPrice(@RequestParam String phone) {
        // Look up customer_prices for this phone (most recent)
        Map<String, Object> priceRecord = orderService.getCustomerPrice(phone);
        if (priceRecord == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("price", null);
            return ApiResponse.success(empty);
        }
        return ApiResponse.success(priceRecord);
    }
}
