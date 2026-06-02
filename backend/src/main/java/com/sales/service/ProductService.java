package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ProductDTO;
import com.sales.entity.Product;
import com.sales.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public ApiResponse<Page<Product>> listProducts(int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        Page<Product> result = productMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getCreatedAt));
        return ApiResponse.success(result);
    }

    public ApiResponse<Product> getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ApiResponse.error("产品不存在");
        }
        return ApiResponse.success(product);
    }

    public ApiResponse<Product> getProductBySlug(String slug) {
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>().eq(Product::getSlug, slug));
        if (product == null) {
            return ApiResponse.error("产品不存在");
        }
        return ApiResponse.success(product);
    }

    @Transactional
    public ApiResponse<Product> createProduct(ProductDTO dto) {
        // 检查slug是否已存在
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSlug, dto.getSlug());
        if (productMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("产品标识已存在");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setSlug(dto.getSlug());
        product.setDescription(dto.getDescription());
        product.setVersion(dto.getVersion());
        product.setDownloadUrl(dto.getDownloadUrl());
        product.setBasePrice(dto.getBasePrice());
        product.setMinPrice(dto.getMinPrice());
        product.setMaxPrice(dto.getMaxPrice());
        product.setAuthEnabled(dto.getAuthEnabled() != null ? dto.getAuthEnabled() : false);
        product.setAuthValidityHours(dto.getAuthValidityHours() != null ? dto.getAuthValidityHours() : 24);
        product.setDefaultCommissionRate(dto.getDefaultCommissionRate());
        product.setStatus("active");

        productMapper.insert(product);
        return ApiResponse.success(product);
    }

    @Transactional
    public ApiResponse<Void> updateProduct(Long id, ProductDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ApiResponse.error("产品不存在");
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getVersion() != null) product.setVersion(dto.getVersion());
        if (dto.getDownloadUrl() != null) product.setDownloadUrl(dto.getDownloadUrl());
        if (dto.getBasePrice() != null) product.setBasePrice(dto.getBasePrice());
        if (dto.getMinPrice() != null) product.setMinPrice(dto.getMinPrice());
        if (dto.getMaxPrice() != null) product.setMaxPrice(dto.getMaxPrice());
        if (dto.getAuthEnabled() != null) product.setAuthEnabled(dto.getAuthEnabled());
        if (dto.getAuthValidityHours() != null) product.setAuthValidityHours(dto.getAuthValidityHours());
        if (dto.getDefaultCommissionRate() != null) product.setDefaultCommissionRate(dto.getDefaultCommissionRate());

        productMapper.updateById(product);
        return ApiResponse.success();
    }

    @Transactional
    public ApiResponse<Void> toggleStatus(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ApiResponse.error("产品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "disabled" : "active");
        productMapper.updateById(product);
        return ApiResponse.success();
    }
}
