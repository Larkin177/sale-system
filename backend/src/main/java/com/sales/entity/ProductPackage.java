package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_packages")
public class ProductPackage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String name;
    private String platform;
    private String version;
    private String description;
    private BigDecimal price;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String downloadUrl;
    private Boolean authEnabled;
    private Integer authValidityHours;
    private Integer sortOrder;
    private String status;

    private String fileUrl;

    private String emailTemplate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
