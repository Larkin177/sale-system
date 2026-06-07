package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ProductPackageDTO;
import com.sales.entity.ProductPackage;
import com.sales.service.ProductPackageService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductPackageController {

    private final ProductPackageService productPackageService;

    // ==================== 管理端接口 ====================

    @GetMapping("/admin/packages")
    public ApiResponse<Page<ProductPackage>> listAdmin(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productPackageService.listPackages(productId, page, size);
    }

    @GetMapping("/admin/packages/{id}")
    public ApiResponse<ProductPackage> get(@PathVariable Long id) {
        return productPackageService.getPackage(id);
    }

    @PostMapping("/admin/packages")
    public ApiResponse<ProductPackage> create(@Valid @RequestBody ProductPackageDTO dto) {
        return productPackageService.createPackage(dto);
    }

    @PutMapping("/admin/packages/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ProductPackageDTO dto) {
        return productPackageService.updatePackage(id, dto);
    }

    @PutMapping("/admin/packages/{id}/toggle-status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        return productPackageService.toggleStatus(id);
    }

    @DeleteMapping("/admin/packages/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return productPackageService.deletePackage(id);
    }

    // ==================== 公开接口 ====================

    @GetMapping("/products/{productId}/packages")
    public ApiResponse<List<ProductPackage>> listPublic(
            @PathVariable Long productId,
            @RequestParam(required = false) String platform) {
        return ApiResponse.success(productPackageService.listActivePackages(productId, platform));
    }

    @GetMapping("/products/{productId}/platforms")
    public ApiResponse<List<String>> listPlatforms(@PathVariable Long productId) {
        return ApiResponse.success(productPackageService.listPlatforms(productId));
    }
}
