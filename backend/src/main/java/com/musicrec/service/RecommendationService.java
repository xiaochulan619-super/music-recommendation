package com.musicrec.service;

import com.musicrec.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecommendationService {

    private final DeepSeekService deepSeekService;
    private final HttpServletRequest request;

    public RecommendationService(DeepSeekService deepSeekService, HttpServletRequest request) {
        this.deepSeekService = deepSeekService;
        this.request = request;
    }

    /**
     * 获取个性化推荐 — 基于 DeepSeek 大模型
     */
    public Result<Map<String, Object>> getRecommendations() {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = deepSeekService.generateRecommend(userId);
        return Result.success(result);
    }
}
