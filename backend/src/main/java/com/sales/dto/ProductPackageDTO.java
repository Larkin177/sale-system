package com.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductPackageDTO {
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @NotBlank(message = "平台不能为空")
    private String platform;

    private String version;
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String downloadUrl;
    private Boolean authEnabled;
    private Integer authValidityHours;
    private Integer sortOrder;
}
