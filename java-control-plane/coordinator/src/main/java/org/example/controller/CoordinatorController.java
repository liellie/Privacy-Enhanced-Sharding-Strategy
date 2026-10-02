package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.entity.Coordinator;
import org.example.entity.CoordinatorNode;
import org.example.service.CoordinatorNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {

    @Autowired
    private CoordinatorNodeService coordinatorNodeService;

    @Autowired
    private CoordinatorNode coordinatorNode;

    @Autowired
    private ApplicationContext context;

    @PostMapping("/init")
    public Result<Coordinator> init() {
        log.info("收到初始化请求：coordinatorId={}", coordinatorNode.getCoordinatorId());
         
        return Result.success(coordinatorNodeService.init(coordinatorNode));
    }

    @PostMapping("/start")
    public Result<Void> start() {
        log.info("收到开始运行请求：coordinatorId={}", coordinatorNode.getCoordinatorId());
        coordinatorNodeService.start();
        return Result.success("started", null);
    }

    @PostMapping("/stop")
    public Result<Void> stop() {
        log.info("收到暂停请求：coordinatorId={}", coordinatorNode.getCoordinatorId());
        coordinatorNodeService.stop();
        return Result.success("stopped", null);
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
}