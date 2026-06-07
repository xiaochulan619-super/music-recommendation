package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_tasks")
public class TrainTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;
}
