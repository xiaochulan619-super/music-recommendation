package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private java.util.List<Comment> replies;
}
