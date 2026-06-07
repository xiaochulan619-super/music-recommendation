package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Select("SELECT s.*, a.name AS artist_name FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE s.title LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY s.id DESC")
    IPage<Song> selectPageWithArtist(Page<Song> page, String keyword);
}
