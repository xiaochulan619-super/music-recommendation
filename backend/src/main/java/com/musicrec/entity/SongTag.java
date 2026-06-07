package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("song_tags")
public class SongTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long songId;
    private Long tagId;
}
