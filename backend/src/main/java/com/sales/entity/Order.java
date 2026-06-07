package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private BigDecimal amount;

    private BigDecimal baseAmount;

    private Long salesId;

    private String status;

    private String paymentMethod;

    private String paymentNo;

    private String customerPhone;

    private String customerEmail;

    private Long claimedBy;

    private LocalDateTime claimedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    // v2: 产品与授权字段
    private Long productId;

    private String productName;

    // v4: 套餐字段
    private Long packageId;

    private String packageName;

    private String platform;

    private String authCode;

    private String authStatus;

    private LocalDateTime authUsedAt;

    private String authMachine;

    private Integer authUses;

    private LocalDateTime authLastValidatedAt;

    // v3: 审核相关字段（静态支付模式）
    private Long reviewedBy;

    private String reviewNote;

    private LocalDateTime reviewedAt;
}
