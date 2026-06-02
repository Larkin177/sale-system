package com.sales.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuthValidateResponse {
    private boolean ok;
    private String error;
    private LocalDateTime expiresAt;
    private Integer uses;
    private String authStatus;
    private String product;
}
