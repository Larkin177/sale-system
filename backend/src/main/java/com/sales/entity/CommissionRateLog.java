package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commission_rate_logs")
public class CommissionRateLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long salesId;
    private BigDecimal oldRate;
    private BigDecimal newRate;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
