package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("customer_prices")
public class CustomerPrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private BigDecimal price;

    private Long salesId;

    private String orderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
