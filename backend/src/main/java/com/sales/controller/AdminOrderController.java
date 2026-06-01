package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.entity.Order;
import com.sales.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<Page<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.listAllOrders(page, size);
    }

    @GetMapping("/unclaimed")
    public ApiResponse<List<Order>> unclaimed() {
        return orderService.listUnclaimedOrders();
    }
}
