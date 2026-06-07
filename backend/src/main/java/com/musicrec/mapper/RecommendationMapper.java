package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.RecommendationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendationMapper extends BaseMapper<RecommendationResult> {

    @Select("SELECT r.score, r.source, s.id AS song_id, s.title, s.cover_url, s.album, " +
            "a.name AS artist_name " +
            "FROM recommendation_results r " +
            "JOIN songs s ON r.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.score DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecsWithSong(@Param("userId") Long userId,
                                                  @Param("limit") int limit);
}
