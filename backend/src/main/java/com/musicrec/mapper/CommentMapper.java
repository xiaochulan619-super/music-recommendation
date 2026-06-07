package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> { }
