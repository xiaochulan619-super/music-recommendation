package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.RatingService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final RatingService ratingService;
    public RatingController(RatingService ratingService) { this.ratingService = ratingService; }

    @PostMapping
    public Result<?> add(@RequestBody Map<String, Object> body) {
        Long songId = Long.valueOf(body.get("songId").toString());
        Integer score = Integer.valueOf(body.get("score").toString());
        return ratingService.addOrUpdate(songId, score);
    }

    @PutMapping("/{songId}")
    public Result<?> update(@PathVariable Long songId, @RequestBody Map<String, Integer> body) {
        return ratingService.addOrUpdate(songId, body.get("score"));
    }
}
