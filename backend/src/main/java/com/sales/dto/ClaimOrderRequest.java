package com.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimOrderRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "手机号不能为空")
    private String phone;
    private String email;
}
