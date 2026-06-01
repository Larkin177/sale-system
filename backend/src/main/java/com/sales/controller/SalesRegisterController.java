package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.dto.SalesRegisterRequest;
import com.sales.entity.Sales;
import com.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesRegisterController {

    private final SalesService salesService;

    @PostMapping("/register")
    public ApiResponse<Sales> register(@Valid @RequestBody SalesRegisterRequest request) {
        return salesService.registerSales(request);
    }
}
