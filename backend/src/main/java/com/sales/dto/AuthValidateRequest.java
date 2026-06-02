package com.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthValidateRequest {
    @NotBlank(message = "授权码不能为空")
    private String authCode;

    @NotBlank(message = "机器指纹不能为空")
    private String machine;
}
