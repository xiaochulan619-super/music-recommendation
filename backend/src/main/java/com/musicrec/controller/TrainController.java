package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.entity.TrainTask;
import com.musicrec.service.TrainService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/train")
public class TrainController {
    private final TrainService trainService;
    public TrainController(TrainService trainService) { this.trainService = trainService; }

    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger() {
        return trainService.triggerTraining();
    }

    @GetMapping("/status")
    public Result<TrainTask> status() {
        return trainService.getStatus();
    }
}
