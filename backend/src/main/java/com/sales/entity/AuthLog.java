package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("auth_logs")
public class AuthLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String authCode;

    private String action;

    private String machine;

    private String ipAddress;

    private String result;

    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
