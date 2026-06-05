package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.SalesDTO;
import com.sales.dto.SalesRegisterRequest;
import com.sales.entity.Sales;
import com.sales.entity.SystemConfig;
import com.sales.mapper.SalesMapper;
import com.sales.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesMapper salesMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiResponse<Sales> createSales(SalesDTO dto) {
        // 检查手机号是否已存在
        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sales::getPhone, dto.getPhone());
        if (salesMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("手机号已存在");
        }

        // 获取默认分润比例
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "default_commission_rate"));
        BigDecimal rate = config != null ? new BigDecimal(config.getConfigValue()) : new BigDecimal("10");

        Sales sales = new Sales();
        sales.setName(dto.getName());
        sales.setPhone(dto.getPhone());
        sales.setPassword(passwordEncoder.encode(dto.getPassword()));
        sales.setCode(generateCode());
        sales.setCommissionRate(dto.getCommissionRate() != null ? dto.getCommissionRate() : rate);
        sales.setStatus("active");

        salesMapper.insert(sales);
        sales.setPassword(null);
        return ApiResponse.success(sales);
    }

    public ApiResponse<Page<Sales>> listSales(int page, int size) {
        Page<Sales> pageParam = new Page<>(page, size);
        Page<Sales> result = salesMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Sales>().orderByDesc(Sales::getCreatedAt));
        return ApiResponse.success(result);
    }

    public ApiResponse<Void> updateSales(Long id, SalesDTO dto) {
        Sales sales = salesMapper.selectById(id);
        if (sales == null) {
            return ApiResponse.error("销售不存在");
        }

        if (dto.getName() != null) sales.setName(dto.getName());
        if (dto.getCommissionRate() != null) sales.setCommissionRate(dto.getCommissionRate());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            sales.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        salesMapper.updateById(sales);
        return ApiResponse.success();
    }

    public ApiResponse<Void> toggleStatus(Long id) {
        Sales sales = salesMapper.selectById(id);
        if (sales == null) {
            return ApiResponse.error("销售不存在");
        }
        sales.setStatus("active".equals(sales.getStatus()) ? "disabled" : "active");
        salesMapper.updateById(sales);
        return ApiResponse.success();
    }

    public Sales getByCode(String code) {
        return salesMapper.selectOne(
                new LambdaQueryWrapper<Sales>().eq(Sales::getCode, code));
    }

    public ApiResponse<Sales> registerSales(SalesRegisterRequest request) {
        // 检查手机号是否已存在
        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sales::getPhone, request.getPhone());
        if (salesMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("该手机号已注册");
        }

        // 获取默认分润比例
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "default_commission_rate"));
        BigDecimal rate = config != null ? new BigDecimal(config.getConfigValue()) : new BigDecimal("10");

        Sales sales = new Sales();
        sales.setName(request.getName());
        sales.setPhone(request.getPhone());
        sales.setPassword(passwordEncoder.encode(request.getPassword()));
        sales.setCode(generateCode());
        sales.setCommissionRate(rate);
        sales.setStatus("active");

        salesMapper.insert(sales);
        sales.setPassword(null);
        return ApiResponse.success(sales);
    }

    private String generateCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (salesMapper.selectCount(
                new LambdaQueryWrapper<Sales>().eq(Sales::getCode, code)) > 0);
        return code;
    }
}
