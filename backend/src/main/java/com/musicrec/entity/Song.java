package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("songs")
public class Song {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long artistId;
    private String album;
    private Integer duration;
    private String coverUrl;
    private String audioUrl;
    private Integer playCount;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String artistName;
}
