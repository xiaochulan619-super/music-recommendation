package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Artist;
import com.musicrec.entity.Song;
import com.musicrec.mapper.ArtistMapper;
import com.musicrec.mapper.SongMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ArtistService {
    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;
    private final SongService songService;

    public ArtistService(ArtistMapper artistMapper, SongMapper songMapper, SongService songService) {
        this.artistMapper = artistMapper;
        this.songMapper = songMapper;
        this.songService = songService;
    }

    public Result<PageResult<Artist>> listArtists(String keyword, int page, int size) {
        Page<Artist> p = new Page<>(page, size);
        LambdaQueryWrapper<Artist> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Artist::getName, keyword).or().like(Artist::getOriginalName, keyword));
        }
        Page<Artist> result = artistMapper.selectPage(p, qw);
        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    public Result<Map<String, Object>> getArtistDetail(Long id) {
        Artist artist = artistMapper.selectById(id);
        if (artist == null) return Result.error("歌手不存在");
        LambdaQueryWrapper<Song> qw = new LambdaQueryWrapper<>();
        qw.eq(Song::getArtistId, id);
        List<Song> songs = songMapper.selectList(qw);
        songService.enrichWithArtists(songs);
        Map<String, Object> data = new HashMap<>();
        data.put("artist", artist);
        data.put("songs", songs);
        return Result.success(data);
    }
}
