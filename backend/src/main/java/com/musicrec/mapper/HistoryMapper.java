package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HistoryMapper extends BaseMapper<PlayHistory> { }
