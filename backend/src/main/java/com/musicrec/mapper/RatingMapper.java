package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT r.song_id, r.score, s.title, a.name AS artist_name " +
            "FROM ratings r " +
            "JOIN songs s ON r.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.score DESC")
    List<Map<String, Object>> selectUserRatingsWithSong(@Param("userId") Long userId);
}
