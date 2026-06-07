package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotNull(message = "packageId required")
    private Long packageId;
    private String salesCode;
    private String email;
    private String paymentMethod;
}
