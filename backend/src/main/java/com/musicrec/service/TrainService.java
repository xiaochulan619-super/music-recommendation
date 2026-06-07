package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.common.Result;
import com.musicrec.entity.TrainTask;
import com.musicrec.mapper.TrainTaskMapper;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrainService {
    private final TrainTaskMapper taskMapper;

    public TrainService(TrainTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public Result<Map<String, Object>> triggerTraining() {
        TrainTask task = new TrainTask();
        task.setStatus("pending");
        taskMapper.insert(task);

        final Long taskId = task.getId();

        new Thread(() -> {
            TrainTask t = taskMapper.selectById(taskId);
            t.setStatus("running");
            t.setStartedAt(LocalDateTime.now());
            taskMapper.updateById(t);
            try {
                ProcessBuilder pb = new ProcessBuilder("python", "main.py");
                pb.directory(new File("../recommendation-engine"));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes());
                int exitCode = p.waitFor();
                t = taskMapper.selectById(taskId);
                t.setStatus(exitCode == 0 ? "completed" : "failed");
                t.setMessage(output.length() > 1000 ? output.substring(0, 1000) : output);
            } catch (Exception e) {
                t = taskMapper.selectById(taskId);
                t.setStatus("failed");
                t.setMessage(e.getMessage());
            }
            t.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(t);
        }).start();

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("status", "pending");
        return Result.success(result);
    }

    public Result<TrainTask> getStatus() {
        LambdaQueryWrapper<TrainTask> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(TrainTask::getId).last("LIMIT 1");
        TrainTask task = taskMapper.selectOne(qw);
        return Result.success(task);
    }
}
