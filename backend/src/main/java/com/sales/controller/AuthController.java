package com.sales.controller;

import com.sales.dto.AdminLoginRequest;
import com.sales.dto.ApiResponse;
import com.sales.dto.LoginRequest;
import com.sales.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sales/login")
    public ApiResponse<Map<String, Object>> salesLogin(@Valid @RequestBody LoginRequest request) {
        return authService.salesLogin(request.getPhone(), request.getPassword());
    }

    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return authService.adminLogin(request.getUsername(), request.getPassword());
    }
}
