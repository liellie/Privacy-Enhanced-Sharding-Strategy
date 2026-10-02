package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.dto.ResultCode;
import org.example.dto.SystemMetrics;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainRequest;
import org.example.service.LoadMetricsReporter;
import org.example.service.TransactionProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private TransactionProcessor transactionProcessor;

    @Autowired
    private LoadMetricsReporter loadMetricsReporter;

    @PostMapping("/transaction/process")
    public Result<ChainReceipt> process(@RequestBody ChainRequest request) {
        try {
            return Result.success(transactionProcessor.process(request));
        } catch (IllegalArgumentException e) {
            log.warn("交易处理参数错误：{}", e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("交易处理失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public Result<SystemMetrics> metrics() {
        return Result.success(loadMetricsReporter.lastMetrics());
    }
}