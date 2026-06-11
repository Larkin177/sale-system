package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_records")
public class SettlementRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long salesId;
    private BigDecimal amount;
    private String status;
    private String proofUrl;
    private String adminNote;
    private Long settledBy;
    private LocalDateTime settledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
