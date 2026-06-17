package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT f.song_id, s.title, s.cover_url, a.name AS artist_name, " +
            "a.nationality, a.original_name AS artist_original_name " +
            "FROM favorites f " +
            "JOIN songs s ON f.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectUserFavoritesWithSong(@Param("userId") Long userId);
}
