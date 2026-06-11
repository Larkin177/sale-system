package com.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SalesRegisterRequest {

    @NotBlank(message = "请输入姓名")
    private String name;

    @NotBlank(message = "请输入邮箱")
    private String email;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String code; // 推广码（可选，不填自动生成）

    @NotBlank(message = "请输入密码")
    private String password;
}
