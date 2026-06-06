package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ProductPackageDTO;
import com.sales.entity.ProductPackage;
import com.sales.mapper.ProductPackageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductPackageService {

    private final ProductPackageMapper productPackageMapper;

    /**
     * 分页查询套餐（管理端）
     */
    public ApiResponse<Page<ProductPackage>> listPackages(Long productId, int page, int size) {
        Page<ProductPackage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProductPackage> wrapper = new LambdaQueryWrapper<ProductPackage>()
                .orderByAsc(ProductPackage::getSortOrder)
                .orderByDesc(ProductPackage::getCreatedAt);
        if (productId != null) {
            wrapper.eq(ProductPackage::getProductId, productId);
        }
        Page<ProductPackage> result = productPackageMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 公开接口：按产品ID和平台查询上架套餐
     */
    public List<ProductPackage> listActivePackages(Long productId, String platform) {
        LambdaQueryWrapper<ProductPackage> wrapper = new LambdaQueryWrapper<ProductPackage>()
                .eq(ProductPackage::getProductId, productId)
                .eq(ProductPackage::getStatus, "active")
                .orderByAsc(ProductPackage::getSortOrder);
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(ProductPackage::getPlatform, platform);
        }
        return productPackageMapper.selectList(wrapper);
    }

    /**
     * 获取所有平台（去重）
     */
    public List<String> listPlatforms(Long productId) {
        LambdaQueryWrapper<ProductPackage> wrapper = new LambdaQueryWrapper<ProductPackage>()
                .eq(ProductPackage::getProductId, productId)
                .eq(ProductPackage::getStatus, "active")
                .select(ProductPackage::getPlatform)
                .groupBy(ProductPackage::getPlatform);
        List<ProductPackage> list = productPackageMapper.selectList(wrapper);
        return list.stream().map(ProductPackage::getPlatform).toList();
    }

    /**
     * 获取单个套餐
     */
    public ApiResponse<ProductPackage> getPackage(Long id) {
        ProductPackage pkg = productPackageMapper.selectById(id);
        if (pkg == null) {
            return ApiResponse.error("套餐不存在");
        }
        return ApiResponse.success(pkg);
    }

    /**
     * 创建套餐
     */
    public ApiResponse<ProductPackage> createPackage(ProductPackageDTO dto) {
        ProductPackage pkg = new ProductPackage();
        pkg.setProductId(dto.getProductId());
        pkg.setName(dto.getName());
        pkg.setPlatform(dto.getPlatform());
        pkg.setVersion(dto.getVersion());
        pkg.setDescription(dto.getDescription());
        pkg.setPrice(dto.getPrice());
        pkg.setMinPrice(dto.getMinPrice() != null ? dto.getMinPrice() : dto.getPrice());
        pkg.setMaxPrice(dto.getMaxPrice() != null ? dto.getMaxPrice() : dto.getPrice());
        pkg.setDownloadUrl(dto.getDownloadUrl());
        pkg.setAuthEnabled(dto.getAuthEnabled() != null ? dto.getAuthEnabled() : true);
        pkg.setAuthValidityHours(dto.getAuthValidityHours() != null ? dto.getAuthValidityHours() : 72);
        pkg.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        pkg.setStatus("active");
        productPackageMapper.insert(pkg);
        return ApiResponse.success(pkg);
    }

    /**
     * 更新套餐
     */
    public ApiResponse<Void> updatePackage(Long id, ProductPackageDTO dto) {
        ProductPackage pkg = productPackageMapper.selectById(id);
        if (pkg == null) {
            return ApiResponse.error("套餐不存在");
        }
        if (dto.getName() != null) pkg.setName(dto.getName());
        if (dto.getPlatform() != null) pkg.setPlatform(dto.getPlatform());
        if (dto.getVersion() != null) pkg.setVersion(dto.getVersion());
        if (dto.getDescription() != null) pkg.setDescription(dto.getDescription());
        if (dto.getPrice() != null) pkg.setPrice(dto.getPrice());
        if (dto.getMinPrice() != null) pkg.setMinPrice(dto.getMinPrice());
        if (dto.getMaxPrice() != null) pkg.setMaxPrice(dto.getMaxPrice());
        if (dto.getDownloadUrl() != null) pkg.setDownloadUrl(dto.getDownloadUrl());
        if (dto.getAuthEnabled() != null) pkg.setAuthEnabled(dto.getAuthEnabled());
        if (dto.getAuthValidityHours() != null) pkg.setAuthValidityHours(dto.getAuthValidityHours());
        if (dto.getSortOrder() != null) pkg.setSortOrder(dto.getSortOrder());
        productPackageMapper.updateById(pkg);
        return ApiResponse.success();
    }

    /**
     * 切换状态
     */
    public ApiResponse<Void> toggleStatus(Long id) {
        ProductPackage pkg = productPackageMapper.selectById(id);
        if (pkg == null) {
            return ApiResponse.error("套餐不存在");
        }
        pkg.setStatus("active".equals(pkg.getStatus()) ? "disabled" : "active");
        productPackageMapper.updateById(pkg);
        return ApiResponse.success();
    }

    /**
     * 删除套餐
     */
    public ApiResponse<Void> deletePackage(Long id) {
        productPackageMapper.deleteById(id);
        return ApiResponse.success();
    }
}
