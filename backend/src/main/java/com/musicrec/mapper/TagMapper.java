package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.id, t.name, t.type, COUNT(*) AS cnt " +
            "FROM favorites f " +
            "JOIN song_tags st ON f.song_id = st.song_id " +
            "JOIN tags t ON st.tag_id = t.id " +
            "WHERE f.user_id = #{userId} " +
            "GROUP BY t.id, t.name, t.type " +
            "ORDER BY cnt DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectUserTopTags(@Param("userId") Long userId,
                                                 @Param("limit") int limit);
}
