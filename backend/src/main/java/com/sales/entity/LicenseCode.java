package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("license_codes")
public class LicenseCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String codeId;

    private Long orderId;

    private Integer consumed;

    private LocalDateTime consumedAt;

    private String consumedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
