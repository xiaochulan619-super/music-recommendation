package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<Tag> { }
