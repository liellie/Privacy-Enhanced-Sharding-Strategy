package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoadStats;
import org.example.dto.Result;
import org.example.dto.ResultCode;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainRequest;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/transaction")
public class ApiController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/submit")
    public Result<ChainReceipt> submit(@RequestBody ChainRequest request) {
        try {
            return Result.success(transactionService.submit(request));
        } catch (IllegalArgumentException e) {
            log.warn("交易参数错误：{}", e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("交易提交失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }

    @GetMapping("/receipt")
    public Result<ChainReceipt> receipt(@RequestParam String requestId) {
        ChainReceipt receipt = transactionService.getReceipt(requestId);
        if (receipt == null) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "receipt not found: " + requestId);
        }
        return Result.success(receipt);
    }

    @PostMapping("/load")
    public Result<LoadStats> load(@RequestParam(defaultValue = "0") int offset,
                                  @RequestParam(defaultValue = "100") int limit) {
        try {
            return Result.success(transactionService.loadFromFile(offset, limit));
        } catch (Exception e) {
            log.error("交易账单批量加载失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }
}