package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.Admin;
import com.sales.entity.Sales;
import com.sales.mapper.AdminMapper;
import com.sales.mapper.SalesMapper;
import com.sales.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SalesMapper salesMapper;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiResponse<Map<String, Object>> salesLogin(String phone, String password) {
        Sales sales = salesMapper.selectOne(
                new LambdaQueryWrapper<Sales>().eq(Sales::getPhone, phone));
        if (sales == null) {
            return ApiResponse.error("手机号或密码错误");
        }
        if (!passwordEncoder.matches(password, sales.getPassword())) {
            return ApiResponse.error("手机号或密码错误");
        }
        if ("disabled".equals(sales.getStatus())) {
            return ApiResponse.error("账号已被禁用");
        }

        String token = jwtUtil.generateToken(sales.getId(), "sales");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("sales", sales);
        return ApiResponse.success(data);
    }

    public ApiResponse<Map<String, Object>> adminLogin(String username, String password) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin == null) {
            return ApiResponse.error("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return ApiResponse.error("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getId(), "admin");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("admin", admin);
        return ApiResponse.success(data);
    }
}
