package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Select("SELECT s.*, a.name AS artist_name, a.nationality, a.original_name AS artist_original_name " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE #{keyword} = '' " +
            "   OR s.title LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR a.name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR a.original_name LIKE CONCAT('%', #{keyword}, '%') " +
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

    @Select("SELECT s.id, s.title, a.name AS artist_name, a.nationality, " +
            "GROUP_CONCAT(t.name SEPARATOR '、') AS tags " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "LEFT JOIN song_tags st ON s.id = st.song_id " +
            "LEFT JOIN tags t ON st.tag_id = t.id " +
            "GROUP BY s.id, s.title, a.name, a.nationality " +
            "ORDER BY s.id")
    List<Map<String, Object>> selectLibrarySummary();

    @Select("<script>" +
            "SELECT s.id AS song_id, s.title, s.cover_url, s.album, s.duration, s.play_count, " +
            "a.name AS artist_name, a.nationality, a.original_name AS artist_original_name " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE s.id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectSongsByIds(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT DISTINCT s.id AS song_id, s.title, s.cover_url, a.name AS artist_name, " +
            "a.nationality, a.original_name AS artist_original_name " +
            "FROM songs s " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "JOIN song_tags st ON s.id = st.song_id " +
            "WHERE st.tag_id IN " +
            "<foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>#{tagId}</foreach>" +
            "AND s.id NOT IN " +
            "<foreach item='exId' collection='excludeIds' open='(' separator=',' close=')'>#{exId}</foreach>" +
            "ORDER BY RAND() " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> selectSongsByTagIds(@Param("tagIds") List<Long> tagIds,
                                                   @Param("excludeIds") List<Long> excludeIds,
                                                   @Param("limit") int limit);
}
