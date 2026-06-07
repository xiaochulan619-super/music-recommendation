package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("artists")
public class Artist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
}
