package com.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimOrderRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    private String phone;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
