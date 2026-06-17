package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Favorite;
import com.musicrec.entity.Song;
import com.musicrec.mapper.FavoriteMapper;
import com.musicrec.mapper.SongMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final SongMapper songMapper;
    private final SongService songService;
    private final HttpServletRequest request;

    public FavoriteService(FavoriteMapper favoriteMapper, SongMapper songMapper,
                           SongService songService, HttpServletRequest request) {
        this.favoriteMapper = favoriteMapper;
        this.songMapper = songMapper;
        this.songService = songService;
        this.request = request;
    }

    public Result<?> addFavorite(Long songId) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Favorite> qw = new LambdaQueryWrapper<>();
        qw.eq(Favorite::getUserId, userId).eq(Favorite::getSongId, songId);
        if (favoriteMapper.selectCount(qw) > 0) return Result.error("已收藏过该歌曲");
        Favorite f = new Favorite();
        f.setUserId(userId); f.setSongId(songId);
        favoriteMapper.insert(f);
        return Result.success("收藏成功");
    }

    public Result<?> removeFavorite(Long songId) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Favorite> qw = new LambdaQueryWrapper<>();
        qw.eq(Favorite::getUserId, userId).eq(Favorite::getSongId, songId);
        favoriteMapper.delete(qw);
        return Result.success("取消收藏");
    }

    public Result<Boolean> checkFavorite(Long songId) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Favorite> qw = new LambdaQueryWrapper<>();
        qw.eq(Favorite::getUserId, userId).eq(Favorite::getSongId, songId);
        return Result.success(favoriteMapper.selectCount(qw) > 0);
    }

    public Result<PageResult<Song>> getFavorites(int page, int size) {
        Long userId = (Long) request.getAttribute("userId");
        Page<Favorite> p = new Page<>(page, size);
        LambdaQueryWrapper<Favorite> qw = new LambdaQueryWrapper<>();
        qw.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt);
        Page<Favorite> result = favoriteMapper.selectPage(p, qw);
        List<Long> songIds = result.getRecords().stream()
                .map(Favorite::getSongId).collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() :
                songMapper.selectBatchIds(songIds);
        songService.enrichWithArtists(songs);
        return Result.success(PageResult.of(result.getTotal(), page, size, songs));
    }
}
