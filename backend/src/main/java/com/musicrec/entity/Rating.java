package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ratings")
public class Rating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;
    private Integer score;
    private LocalDateTime createdAt;
}
