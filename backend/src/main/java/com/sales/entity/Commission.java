package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commissions")
public class Commission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long salesId;

    private BigDecimal amount;

    private BigDecimal adminAmount;

    private BigDecimal rate;

    private String status;

    private LocalDateTime settledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
