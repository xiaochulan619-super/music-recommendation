package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.*;
import com.musicrec.mapper.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SongService {
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;

    public SongService(SongMapper songMapper, ArtistMapper artistMapper) {
        this.songMapper = songMapper;
        this.artistMapper = artistMapper;
    }

    public Result<PageResult<Song>> listSongs(String keyword, int page, int size) {
        Page<Song> p = new Page<>(page, size);
        IPage<Song> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = songMapper.selectPageWithArtist(p, keyword);
        } else {
            result = songMapper.selectPageWithArtist(p, "");
        }
        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    public Result<Song> getSongDetail(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) return Result.error("歌曲不存在");
        if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) song.setArtistName(artist.getName());
        }
        return Result.success(song);
    }
}
