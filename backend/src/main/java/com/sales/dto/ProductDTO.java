package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @NotBlank(message = "产品标识不能为空")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "产品标识只能包含小写字母、数字和连字符")
    private String slug;

    private String description;

    private String version;

    private String downloadUrl;

    @NotNull(message = "基础价格不能为空")
    @DecimalMin(value = "0.01", message = "基础价格必须大于0")
    private BigDecimal basePrice;

    @DecimalMin(value = "0", message = "最低定价不能小于0")
    private BigDecimal minPrice;

    @DecimalMin(value = "0", message = "最高定价不能小于0")
    private BigDecimal maxPrice;

    private Boolean authEnabled;

    private Integer authValidityHours;

    @DecimalMin(value = "0", message = "分润比例不能小于0")
    @DecimalMax(value = "100", message = "分润比例不能大于100")
    private BigDecimal defaultCommissionRate;
}
