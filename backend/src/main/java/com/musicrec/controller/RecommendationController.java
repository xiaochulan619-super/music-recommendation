package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.RecommendationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recService;
    public RecommendationController(RecommendationService recService) { this.recService = recService; }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "20") int limit) {
        return recService.getRecommendations(limit);
    }
}
