package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recommendation_results")
public class RecommendationResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;
    private Float score;
    private String source;
    private LocalDateTime createdAt;
}
