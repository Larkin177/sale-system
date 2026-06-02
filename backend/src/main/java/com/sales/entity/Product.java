package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    private String description;

    private String version;

    private String downloadUrl;

    private BigDecimal basePrice;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Boolean authEnabled;

    private Integer authValidityHours;

    private BigDecimal defaultCommissionRate;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
