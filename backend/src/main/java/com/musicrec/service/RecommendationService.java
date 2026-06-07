package com.musicrec.service;

import com.musicrec.common.Result;
import com.musicrec.mapper.RecommendationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private final RecommendationMapper recMapper;
    private final HttpServletRequest request;

    public RecommendationService(RecommendationMapper recMapper, HttpServletRequest request) {
        this.recMapper = recMapper;
        this.request = request;
    }

    public Result<List<Map<String, Object>>> getRecommendations(int limit) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(recMapper.selectRecsWithSong(userId, limit));
    }
}
