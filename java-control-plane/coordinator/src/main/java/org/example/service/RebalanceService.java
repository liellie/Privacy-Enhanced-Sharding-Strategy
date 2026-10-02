package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.config.CoordinatorLifecycle;
import org.example.entity.Coordinator;
import org.example.entity.Shard;
import org.example.enums.CoordinatorState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RebalanceService {

    @Value("${coordinator.scheduler.enabled}")
    private boolean enabled;

    @Value("${coordinator.rebalance.tps-threshold}")
    private long tpsThreshold;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskCoordinatorService taskCoordinatorService;

    @Autowired
    private CoordinatorLifecycle coordinatorLifecycle;

    @Scheduled(fixedDelayString = "${coordinator.epoch-duration}")
    public void scheduledCheck() {
        if (!enabled) {
            return;
        }
         
        if (!coordinatorLifecycle.isReady()) {
            return;
        }
        checkAndRebalance();
    }

    public void checkAndRebalance() {
         
        Coordinator global = coordinatorService.getOne(
                new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
        if (global == null || global.getGlobalState() != CoordinatorState.RUNNING) {
            log.debug("协调者未就绪或状态={}，跳过再平衡检查", global == null ? null : global.getGlobalState());
            return;
        }

        Shard busy = shardService.getBusiestShard();
        Shard idle = shardService.getMostIdleShard();
        if (busy == null || idle == null || busy.getShardId().equals(idle.getShardId())) {
            log.debug("分片不足两个或负载均衡（busy={}, idle={}），无需再平衡", busy, idle);
            return;
        }

        long busyTps = busy.getCurrentTps() == null ? 0 : busy.getCurrentTps();
        long idleTps = idle.getCurrentTps() == null ? 0 : idle.getCurrentTps();
        long gap = busyTps - idleTps;
        if (gap <= tpsThreshold) {
            log.debug("负载均衡：busy(shard{})={} tps, idle(shard{})={} tps, 差值 {} ≤ 阈值 {}，无需再平衡",
                    busy.getShardId(), busyTps, idle.getShardId(), idleTps, gap, tpsThreshold);
            return;
        }

        log.info("检测到负载失衡：busy(shard{})={} tps, idle(shard{})={} tps, 差值 {} > 阈值 {}，触发再平衡",
                busy.getShardId(), busyTps, idle.getShardId(), idleTps, gap, tpsThreshold);
        coordinatorService.updateGlobalState(CoordinatorState.REBALANCING);
        try {
            String requestId = "rebalance-" + System.currentTimeMillis();
             
            taskService.createRebalanceTask(requestId, busy.getShardId(), idle.getShardId());
            log.info("再平衡任务已创建：{}，sourceShard={} → targetShard={}", requestId, busy.getShardId(), idle.getShardId());
             
            taskCoordinatorService.processPending();
             
            int totalShards = shardService.listAll().size();
            coordinatorService.updateShardCount(totalShards);
        } catch (Exception e) {
             
            log.error("再平衡流程异常，恢复 RUNNING：{}", e.getMessage(), e);
        } finally {
             
            coordinatorService.updateGlobalState(CoordinatorState.RUNNING);
        }
    }
}