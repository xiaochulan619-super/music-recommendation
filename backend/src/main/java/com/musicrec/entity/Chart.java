package com.musicrec.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("charts")
public class Chart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long songId;
    private String chartType;
    private Integer playCount;
    private Integer favCount;
    private Float hotScore;
    private LocalDate rankDate;
}
