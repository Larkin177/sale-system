package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
