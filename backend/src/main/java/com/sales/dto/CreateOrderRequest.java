package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    private String salesCode;

    private String phone;

    private String paymentMethod;
}
