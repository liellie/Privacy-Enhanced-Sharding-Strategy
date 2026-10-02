package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.service.ExperimentDataService;
import org.example.service.ExperimentFinishService;
import org.example.service.NodeCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/execute")
public class ExecuteController {

    @Autowired
    private NodeCommandService nodeCommandService;

    @Autowired
    private ExperimentDataService experimentDataService;

    @Autowired
    private ExperimentFinishService experimentFinishService;

    @GetMapping("/init")
    public String init() {
        log.info("[SYSTEM-CONTROLLER] 收到初始化指令");
        nodeCommandService.initAll();
        return "success";
    }

    @GetMapping("/start")
    public String start() {
        log.info("[SYSTEM-CONTROLLER] 收到开始运行指令");
        nodeCommandService.startAll();
        return "success";
    }

    @GetMapping("/stop")
    public String stop() {
        log.info("[SYSTEM-CONTROLLER] 收到暂停指令");
        nodeCommandService.stopAll();
        return "success";
    }

    @GetMapping("/shutdown")
    public String shutdown() {
        log.info("[SYSTEM-CONTROLLER] 收到停止指令");
        nodeCommandService.shutdownAll();
        return "success";
    }

    @GetMapping("/finish")
    public Result<String> finish() {
        log.info("[SYSTEM-CONTROLLER] 收到结束实验指令");
        String reportPath = experimentFinishService.finish("手动结束（/execute/finish）");
        if (reportPath == null) {
            return Result.error(org.example.dto.ResultCode.INTERNAL_ERROR.getCode(),
                    "结束失败：协调者未初始化，或实验已处于 FINISHED 状态");
        }
        return Result.success("finished, report=" + reportPath, reportPath);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(experimentDataService.status());
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(experimentDataService.stats());
    }
}