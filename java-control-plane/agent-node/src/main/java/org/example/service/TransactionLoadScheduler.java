package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoadStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class TransactionLoadScheduler {

    @Autowired
    private TransactionService transactionService;

    @Value("${agent.scheduler.enabled}")
    private boolean enabled;

    @Value("${agent.scheduler.batch-size}")
    private int batchSize;

    private final AtomicLong nextOffset = new AtomicLong(0);

    private final AtomicBoolean finished = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${agent.scheduler.interval-ms}")
    public void loadBatch() {
         
        if (!enabled) {
            return;
        }
         
        if (finished.get()) {
            log.info("交易定时加载：账单已全部加载完成（finished=true），定时加载停止。");
            return;
        }

        long offset = nextOffset.get();
        log.info("交易定时加载：开始加载，offset={}, batchSize={}", offset, batchSize);
        try {
            LoadStats stats = transactionService.loadFromFile((int) offset, batchSize);
             
            nextOffset.set(stats.getLastLineNo());
            if (stats.isFinished()) {
                finished.set(true);
                log.info("交易定时加载：账单全部加载完成，累计行号={}, 本批 submitted={}",
                        stats.getLastLineNo(), stats.getSubmitted());
            }
        } catch (Exception e) {
             
            log.error("交易定时加载：本批加载失败（下次触发重试）：{}", e.getMessage(), e);
        }
    }

    public boolean isFinished() {
        return finished.get();
    }

    public long getLastLineNo() {
        return nextOffset.get();
    }
}