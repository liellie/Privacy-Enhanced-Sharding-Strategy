package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.config.CoordinatorLifecycle;
import org.example.entity.Coordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoordinatorNodeService {

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private CoordinatorLifecycle coordinatorLifecycle;

    public Coordinator init(Coordinator coordinator) {
        String coordinatorId = coordinator.getCoordinatorId();
        Long epochDuration = coordinator.getEpochDuration();
         
        if (coordinatorId == null || coordinatorId.isBlank()) {
            throw new IllegalArgumentException("参数未配置（coordinatorId='" + coordinatorId + "'）");
        }
        if (epochDuration == null || epochDuration <= 0) {
            throw new IllegalArgumentException("参数未配置（epochDuration=" + epochDuration + "）");
        }
        log.info("=== 协调者节点初始化开始，coordinatorId={}, epochDuration={}ms ===",
                coordinatorId, epochDuration);

        Coordinator coordinatorInstance = coordinatorService.start();
         
        if (!coordinatorId.equals(coordinatorInstance.getCoordinatorId())) {
            throw new IllegalStateException(String.format(
                    "配置的 coordinatorId='%s' 与全局协调者记录不一致（实际='%s'），请检查配置",
                    coordinatorId, coordinatorInstance.getCoordinatorId()));
        }
        log.info("协调者就绪：coordinatorId={}, epoch={}, state={}",
                coordinatorInstance.getCoordinatorId(), coordinatorInstance.getCurrentEpoch(), coordinatorInstance.getGlobalState());

        coordinatorService.updateEpochDuration(epochDuration);
        log.info("Epoch 持续时间已应用：epochDuration={}ms", epochDuration);

        int totalShards = shardService.listAll().size();
        coordinatorService.updateShardCount(totalShards);
        log.info("已同步分片总数：totalShards={}", totalShards);

        log.info("=== 协调者节点初始化完成：coordinatorId={}, epoch={} ===",
                coordinatorInstance.getCoordinatorId(), coordinatorInstance.getCurrentEpoch());
         
        coordinatorLifecycle.markInitialized();
        return coordinatorInstance;
    }

    public void start() {
        if (!coordinatorLifecycle.isInitialized()) {
            throw new IllegalStateException("协调者尚未初始化，请先执行 init 命令（或调用 POST /coordinator/init）");
        }
        coordinatorLifecycle.markStarted();
        log.info("=== 协调者已开始运行：Epoch 推进 / 任务协调 / 再平衡等定时任务已激活 ===");
    }

    public void stop() {
        coordinatorLifecycle.markStopped();
        log.info("=== 协调者已暂停：定时任务已关闭（可执行 start 恢复） ===");
    }
}