package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sales_payment_codes")
public class SalesPaymentCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long salesId;
    private String codeType;
    private String codeUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
