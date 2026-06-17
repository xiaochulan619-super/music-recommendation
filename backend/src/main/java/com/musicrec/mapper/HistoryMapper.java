package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface HistoryMapper extends BaseMapper<PlayHistory> {

    @Select("SELECT h.song_id, s.title, a.name AS artist_name, COUNT(*) AS play_count " +
            "FROM play_history h " +
            "JOIN songs s ON h.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE h.user_id = #{userId} " +
            "GROUP BY h.song_id, s.title, a.name " +
            "ORDER BY play_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectUserHistoryWithCount(@Param("userId") Long userId,
                                                          @Param("limit") int limit);
}
