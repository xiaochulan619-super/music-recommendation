package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.PlayHistory;
import com.musicrec.entity.Song;
import com.musicrec.mapper.HistoryMapper;
import com.musicrec.mapper.SongMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {
    private final HistoryMapper historyMapper;
    private final SongMapper songMapper;
    private final HttpServletRequest request;

    public HistoryService(HistoryMapper historyMapper, SongMapper songMapper,
                          HttpServletRequest request) {
        this.historyMapper = historyMapper;
        this.songMapper = songMapper;
        this.request = request;
    }

    public Result<?> recordPlay(Long songId) {
        Long userId = (Long) request.getAttribute("userId");
        PlayHistory h = new PlayHistory();
        h.setUserId(userId); h.setSongId(songId);
        historyMapper.insert(h);
        return Result.success();
    }

    public Result<PageResult<Song>> getHistory(int page, int size) {
        Long userId = (Long) request.getAttribute("userId");
        Page<PlayHistory> p = new Page<>(page, size);
        LambdaQueryWrapper<PlayHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(PlayHistory::getUserId, userId).orderByDesc(PlayHistory::getPlayedAt);
        Page<PlayHistory> result = historyMapper.selectPage(p, qw);
        List<Long> songIds = result.getRecords().stream()
                .map(PlayHistory::getSongId).distinct().collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() :
                songMapper.selectBatchIds(songIds);
        return Result.success(PageResult.of(result.getTotal(), page, size, songs));
    }
}
