package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.*;
import com.musicrec.mapper.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SongService {
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;

    /** 华语歌手国籍关键词 */
    private static final Pattern CHINESE_PATTERN =
            Pattern.compile(".*(中国|台湾|香港|新加坡|马来西亚).*");

    public SongService(SongMapper songMapper, ArtistMapper artistMapper) {
        this.songMapper = songMapper;
        this.artistMapper = artistMapper;
    }

    /** 判断是否应显示中文名 */
    private boolean isChineseNationality(String nationality) {
        return nationality != null && CHINESE_PATTERN.matcher(nationality).matches();
    }

    /** 获取歌手显示名：华语歌手用 originalName，否则用 name */
    private void applyDisplayName(Song song, Artist artist) {
        song.setNationality(artist.getNationality());
        song.setArtistOriginalName(artist.getOriginalName());
        if (isChineseNationality(artist.getNationality())
                && artist.getOriginalName() != null && !artist.getOriginalName().isEmpty()) {
            song.setArtistName(artist.getOriginalName());
        } else {
            song.setArtistName(artist.getName());
        }
    }

    public Result<PageResult<Song>> listSongs(String keyword, int page, int size) {
        Page<Song> p = new Page<>(page, size);
        IPage<Song> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = songMapper.selectPageWithArtist(p, keyword);
        } else {
            // 发现页默认排序：用排序分数（中文歌加权 + 播放量），让热门中文歌优先展示
            result = songMapper.selectPageSorted(p);
        }
        enrichWithArtists(result.getRecords());

        // 发现页默认排序：贪心构建多样化列表
        if (keyword == null || keyword.isEmpty()) {
            List<Song> diverse = SongDiversityUtil.greedyDiverse(
                    result.getRecords(), result.getRecords().size(), Song::getAlbum, Song::getArtistName);
            result.getRecords().clear();
            result.getRecords().addAll(diverse);
        }

        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    /**
     * 推荐流：中文歌占比 60-70%，排序多样化，同专辑/同歌手不连续
     * 用于首页"为你推荐"轮播
     */
    public Result<List<Song>> listRecommendSongs(int limit) {
        // 1. 取足够多的候选
        List<Song> pool = songMapper.selectList(
                new LambdaQueryWrapper<Song>()
                        .orderByDesc(Song::getPlayCount)
                        .last("LIMIT " + (limit * 4))
        );
        enrichWithArtists(pool);

        // 2. 分离中文和外文，各自按播放量排序
        List<Song> cnPool = pool.stream()
                .filter(s -> SongDiversityUtil.isChinese(s.getNationality()))
                .sorted((a, b) -> Integer.compare(
                        b.getPlayCount() != null ? b.getPlayCount() : 0,
                        a.getPlayCount() != null ? a.getPlayCount() : 0))
                .collect(Collectors.toList());
        List<Song> otherPool = pool.stream()
                .filter(s -> !SongDiversityUtil.isChinese(s.getNationality()))
                .sorted((a, b) -> Integer.compare(
                        b.getPlayCount() != null ? b.getPlayCount() : 0,
                        a.getPlayCount() != null ? a.getPlayCount() : 0))
                .collect(Collectors.toList());

        // 3. 各自内部贪心打散
        cnPool = SongDiversityUtil.greedyDiverse(cnPool, cnPool.size(), Song::getAlbum, Song::getArtistName);
        otherPool = SongDiversityUtil.greedyDiverse(otherPool, otherPool.size(), Song::getAlbum, Song::getArtistName);

        // 4. 交错合并：每 3 首中文插 1 首外语
        List<Song> result = new ArrayList<>();
        int cnIdx = 0, otherIdx = 0;
        while (result.size() < limit) {
            // 中文块
            for (int k = 0; k < 3 && cnIdx < cnPool.size() && result.size() < limit; k++) {
                result.add(cnPool.get(cnIdx++));
            }
            // 外语插入
            if (otherIdx < otherPool.size() && result.size() < limit) {
                result.add(otherPool.get(otherIdx++));
            } else if (cnIdx >= cnPool.size()) {
                // 中文用完了，填外语
                while (result.size() < limit && otherIdx < otherPool.size()) {
                    result.add(otherPool.get(otherIdx++));
                }
            }
        }

        // 5. 交错后可能产生新的连续冲突，再做一次打散
        result = SongDiversityUtil.greedyDiverse(result, limit, Song::getAlbum, Song::getArtistName);

        return Result.success(result);
    }

    /**
     * 批量填充歌曲的歌手信息（artistName, nationality, artistOriginalName）
     * 华语歌手自动使用中文名
     */
    public void enrichWithArtists(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;
        List<Long> artistIds = songs.stream()
                .map(Song::getArtistId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (artistIds.isEmpty()) return;
        Map<Long, Artist> artistMap = artistMapper.selectBatchIds(artistIds)
                .stream().collect(Collectors.toMap(Artist::getId, a -> a));
        for (Song song : songs) {
            Artist artist = artistMap.get(song.getArtistId());
            if (artist != null) {
                applyDisplayName(song, artist);
            }
        }
    }

    public Result<Song> getSongDetail(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) return Result.error("歌曲不存在");
        if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                applyDisplayName(song, artist);
            }
        }
        return Result.success(song);
    }
}
