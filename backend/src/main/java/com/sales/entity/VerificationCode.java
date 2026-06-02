package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("verification_codes")
public class VerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String code;

    private String purpose;

    private Boolean used;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
