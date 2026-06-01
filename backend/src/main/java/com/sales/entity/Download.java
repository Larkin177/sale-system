package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("downloads")
public class Download {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String downloadToken;

    private Integer downloadCount;

    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
