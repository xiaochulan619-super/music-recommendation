package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.common.Result;
import com.musicrec.entity.Rating;
import com.musicrec.mapper.RatingMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RatingService {
    private final RatingMapper ratingMapper;
    private final HttpServletRequest request;

    public RatingService(RatingMapper ratingMapper, HttpServletRequest request) {
        this.ratingMapper = ratingMapper;
        this.request = request;
    }

    public Result<?> addOrUpdate(Long songId, Integer score) {
        Long userId = (Long) request.getAttribute("userId");
        if (score < 1 || score > 5) return Result.error("评分范围 1~5");
        LambdaQueryWrapper<Rating> qw = new LambdaQueryWrapper<>();
        qw.eq(Rating::getUserId, userId).eq(Rating::getSongId, songId);
        Rating existing = ratingMapper.selectOne(qw);
        if (existing != null) {
            existing.setScore(score);
            ratingMapper.updateById(existing);
        } else {
            Rating r = new Rating();
            r.setUserId(userId); r.setSongId(songId); r.setScore(score);
            ratingMapper.insert(r);
        }
        return Result.success("评分成功");
    }
}
