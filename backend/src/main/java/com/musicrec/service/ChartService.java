package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Chart;
import com.musicrec.entity.Song;
import com.musicrec.mapper.ChartMapper;
import com.musicrec.mapper.SongMapper;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartService {
    private final ChartMapper chartMapper;
    private final SongMapper songMapper;

    public ChartService(ChartMapper chartMapper, SongMapper songMapper) {
        this.chartMapper = chartMapper;
        this.songMapper = songMapper;
    }

    public Result<List<Map<String, Object>>> getCharts(String type, int limit) {
        LambdaQueryWrapper<Chart> qw = new LambdaQueryWrapper<>();
        qw.eq(Chart::getChartType, type != null ? type : "total")
                .orderByDesc(Chart::getHotScore).last("LIMIT " + limit);
        List<Chart> charts = chartMapper.selectList(qw);

        List<Long> songIds = charts.stream().map(Chart::getSongId).collect(Collectors.toList());
        Map<Long, Song> songMap = songIds.isEmpty() ? Map.of() :
                songMapper.selectBatchIds(songIds).stream()
                        .collect(Collectors.toMap(Song::getId, s -> s));

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < charts.size(); i++) {
            Chart c = charts.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("song", songMap.get(c.getSongId()));
            item.put("playCount", c.getPlayCount());
            item.put("favCount", c.getFavCount());
            item.put("hotScore", c.getHotScore());
            list.add(item);
        }
        return Result.success(list);
    }
}
