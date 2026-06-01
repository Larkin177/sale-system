package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalesDTO {
    private Long id;

    @NotBlank(message = "销售名称不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String password;

    @DecimalMin(value = "0", message = "分润比例不能小于0")
    @DecimalMax(value = "100", message = "分润比例不能大于100")
    private BigDecimal commissionRate;
}
