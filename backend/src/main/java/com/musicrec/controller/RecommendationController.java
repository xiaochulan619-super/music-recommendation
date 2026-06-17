package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recService;

    public RecommendationController(RecommendationService recService) {
        this.recService = recService;
    }

    /**
     * 获取 AI 个性化推荐
     * POST 方法，触发实时推荐（含 DeepSeek API 调用）
     */
    @PostMapping
    public Result<Map<String, Object>> recommend() {
        return recService.getRecommendations();
    }

    /**
     * 获取推荐结果（GET 同样触发实时推荐）
     */
    @GetMapping
    public Result<Map<String, Object>> getLatest() {
        return recService.getRecommendations();
    }
}
