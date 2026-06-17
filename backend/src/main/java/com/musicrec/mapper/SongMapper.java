package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Select("SELECT s.*, a.name AS artist_name, a.nationality, a.original_name AS artist_original_name " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE #{keyword} = '' " +
            "   OR s.title LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR a.name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR s.id IN (SELECT st.song_id FROM song_tags st JOIN tags t ON st.tag_id = t.id WHERE t.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY s.id DESC")
    IPage<Song> selectPageWithArtist(Page<Song> page, String keyword);

    /**
     * 按播放量降序分页查询（发现页默认排序）
     */
    @Select("SELECT s.*, a.name AS artist_name, a.nationality, a.original_name AS artist_original_name " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "ORDER BY s.play_count DESC")
    IPage<Song> selectPageSorted(Page<Song> page);
}
