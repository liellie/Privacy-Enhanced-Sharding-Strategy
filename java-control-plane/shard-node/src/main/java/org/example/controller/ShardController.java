package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.entity.Shard;
import org.example.entity.ShardNode;
import org.example.service.ShardNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/shard")
public class ShardController {

    @Autowired
    private ShardNodeService shardNodeService;

    @Autowired
    private ShardNode shardNode;

    @Autowired
    private ApplicationContext context;

    @PostMapping("/init")
    public Result<Shard> init() {
        log.info("收到初始化请求：ip={}, port={}", shardNode.getIpAddress(), shardNode.getPort());
         
        return Result.success(shardNodeService.init(shardNode));
    }

    @PostMapping("/start")
    public Result<Void> start() {
        log.info("收到开始运行请求：ip={}, port={}", shardNode.getIpAddress(), shardNode.getPort());
        shardNodeService.start();
        return Result.success("started", null);
    }

    @PostMapping("/stop")
    public Result<Void> stop() {
        log.info("收到暂停请求：ip={}, port={}", shardNode.getIpAddress(), shardNode.getPort());
        shardNodeService.stop();
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