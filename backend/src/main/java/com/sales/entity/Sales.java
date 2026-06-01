package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales")
public class Sales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    private String password;

    private String code;

    private BigDecimal commissionRate;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
