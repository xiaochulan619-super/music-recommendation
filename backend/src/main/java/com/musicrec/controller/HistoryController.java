package com.musicrec.controller;

import com.musicrec.common.PageResult;
import com.musicrec.common.Result;
import com.musicrec.entity.Song;
import com.musicrec.service.HistoryService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService historyService;
    public HistoryController(HistoryService historyService) { this.historyService = historyService; }

    @PostMapping
    public Result<?> record(@RequestBody Map<String, Long> body) {
        return historyService.recordPlay(body.get("songId"));
    }

    @GetMapping
    public Result<PageResult<Song>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.getHistory(page, size);
    }
}
