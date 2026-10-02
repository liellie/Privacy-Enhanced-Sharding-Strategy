package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.entity.Agent;
import org.example.entity.AgentNode;
import org.example.service.ExperimentOpsService;
import org.example.service.NodeService;
import org.example.service.TransactionLoadScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private AgentNode agentNode;

    @Autowired
    private TransactionLoadScheduler transactionLoadScheduler;

    @Autowired
    private ExperimentOpsService experimentOpsService;

    @Autowired
    private ApplicationContext context;

    @PostMapping("/init")
    public Result<Agent> init() {
        log.info("收到初始化请求：agentId={}", agentNode.getAgentId());
         
        return Result.success(nodeService.initialize(agentNode));
    }

    @PostMapping("/shutdown")
    public Result<Void> shutdown() {
        log.info("收到关闭请求，正在关闭本节点...");
        new Thread(() -> {
            try {
                 
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            int exitCode = SpringApplication.exit(context, () -> 0);
            System.exit(exitCode);
        }, "node-shutdown").start();
        return Result.success("shutting down", null);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        log.info("收到账单状态查询请求");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("finished", transactionLoadScheduler.isFinished());
        result.put("lastLineNo", transactionLoadScheduler.getLastLineNo());
        return Result.success(result);
    }

    @PostMapping("/experiment/start")
    public Result<Map<String, Object>> experimentStart() {
        log.info("收到实验批量操作启动请求");
        return Result.success(experimentOpsService.start());
    }

    @PostMapping("/experiment/stop")
    public Result<Map<String, Object>> experimentStop() {
        log.info("收到实验批量操作停止请求");
        return Result.success(experimentOpsService.stop());
    }

    @GetMapping("/experiment/status")
    public Result<Map<String, Object>> experimentStatus() {
        return Result.success(experimentOpsService.status());
    }
}