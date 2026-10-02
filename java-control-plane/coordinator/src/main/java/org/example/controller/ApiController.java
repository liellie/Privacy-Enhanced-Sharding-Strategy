package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.dto.Result;
import org.example.dto.ResultCode;
import org.example.entity.Coordinator;
import org.example.entity.Task;
import org.example.service.CoordinatorService;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BlockchainService blockchainService;

    @GetMapping("/epoch")
    public Result<Coordinator> epoch() {
        Coordinator global = coordinatorService.getById("coordinator-1");
        if (global == null) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "协调者尚未初始化，请先调用 POST /coordinator/init");
        }
        return Result.success(global);
    }

    @GetMapping("/tasks/pending")
    public Result<List<Task>> pendingTasks() {
        return Result.success(taskService.listPendingTasks());
    }

    @GetMapping("/metrics")
    public Result<Map<String, BigInteger>> metrics() {
        try {
            Map<String, BigInteger> chain = Map.of(
                    "loadImbalance", blockchainService.loadImbalance(),
                    "privacyRisk", blockchainService.privacyRisk());
            return Result.success(chain);
        } catch (Exception e) {
            log.warn("链上指标查询失败：{}", e.getMessage());
            return Result.error(ResultCode.INTERNAL_ERROR.getCode(), "链上指标查询失败：" + e.getMessage());
        }
    }
}