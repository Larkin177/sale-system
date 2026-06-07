package com.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tutorials")
public class Tutorial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String type;

    private String content;

    private String mediaUrl;

    private String thumbnailUrl;

    private String category;

    private Integer sortOrder;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
