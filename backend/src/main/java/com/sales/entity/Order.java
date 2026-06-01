package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private BigDecimal amount;

    private BigDecimal baseAmount;

    private Long salesId;

    private String status;

    private String paymentMethod;

    private String paymentNo;

    private String customerPhone;

    private Long claimedBy;

    private LocalDateTime claimedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
