package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.ChartService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
public class ChartController {
    private final ChartService chartService;
    public ChartController(ChartService chartService) { this.chartService = chartService; }

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "total") String type,
            @RequestParam(defaultValue = "50") int limit) {
        return chartService.getCharts(type, limit);
    }
}
