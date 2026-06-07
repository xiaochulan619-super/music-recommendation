package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Song;
import com.musicrec.entity.SongTag;
import com.musicrec.entity.Tag;
import com.musicrec.mapper.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagMapper tagMapper;
    private final SongTagMapper songTagMapper;
    private final SongMapper songMapper;

    public TagService(TagMapper tagMapper, SongTagMapper songTagMapper, SongMapper songMapper) {
        this.tagMapper = tagMapper;
        this.songTagMapper = songTagMapper;
        this.songMapper = songMapper;
    }

    public Result<List<Tag>> listTags(String type) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) qw.eq(Tag::getType, type);
        return Result.success(tagMapper.selectList(qw));
    }

    public Result<PageResult<Song>> getSongsByTag(Long tagId, int page, int size) {
        LambdaQueryWrapper<SongTag> qw = new LambdaQueryWrapper<>();
        qw.eq(SongTag::getTagId, tagId);
        Page<SongTag> p = new Page<>(page, size);
        Page<SongTag> result = songTagMapper.selectPage(p, qw);
        List<Long> songIds = result.getRecords().stream()
                .map(SongTag::getSongId).collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() :
                songMapper.selectBatchIds(songIds);
        return Result.success(PageResult.of(result.getTotal(), page, size, songs));
    }
}
