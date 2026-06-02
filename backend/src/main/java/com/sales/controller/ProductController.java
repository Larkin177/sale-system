package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ProductDTO;
import com.sales.entity.Product;
import com.sales.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productService.listProducts(page, size);
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> get(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping
    public ApiResponse<Product> create(@Valid @RequestBody ProductDTO dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        return productService.updateProduct(id, dto);
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        return productService.toggleStatus(id);
    }
}
