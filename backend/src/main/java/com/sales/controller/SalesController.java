package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.SalesDTO;
import com.sales.entity.Sales;
import com.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    public ApiResponse<Sales> create(@Valid @RequestBody SalesDTO dto) {
        return salesService.createSales(dto);
    }

    @GetMapping
    public ApiResponse<Page<Sales>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return salesService.listSales(page, size);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SalesDTO dto) {
        return salesService.updateSales(id, dto);
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        return salesService.toggleStatus(id);
    }
}
