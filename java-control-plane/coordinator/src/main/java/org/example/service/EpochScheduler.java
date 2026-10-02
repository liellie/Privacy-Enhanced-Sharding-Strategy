package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.config.CoordinatorLifecycle;
import org.example.entity.ChainTranscription;
import org.example.entity.Coordinator;
import org.example.enums.CoordinatorState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
public class EpochScheduler {

    @Value("${coordinator.scheduler.enabled}")
    private boolean enabled;

    @Value("${coordinator.experiment.shard-count:4}")
    private int shardCount;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ChainTranscriptionService chainTranscriptionService;

    @Autowired
    private CoordinatorLifecycle coordinatorLifecycle;

    @Autowired
    private EpochGasRecorder epochGasRecorder;

    @Scheduled(fixedDelayString = "${coordinator.epoch-duration}")
    public void advance() {
         
        if (!enabled) {
            return;
        }
         
        if (!coordinatorLifecycle.isReady()) {
            return;
        }
         
        Coordinator global = coordinatorService.getOne(
                new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
        if (global == null) {
            log.debug("协调者尚未初始化（coordinator 表为空），跳过 Epoch 推进");
            return;
        }
         
        if (global.getGlobalState() != CoordinatorState.RUNNING) {
            log.debug("全局状态={}，跳过 Epoch 推进", global.getGlobalState());
            return;
        }

        Long currentEpoch = global.getCurrentEpoch();
        List<EpochGasStep> steps = new ArrayList<>();
        int n = Math.max(1, shardCount);

        BigInteger target = chainEpoch()
                .map(e -> e.add(BigInteger.ONE))
                .orElseGet(() -> BigInteger.valueOf(currentEpoch + 1));

        steps.add(runStep("beacon_finalize",
                () -> blockchainService.beaconFinalizeInfo(target)));

        for (int s = 0; s < n; s++) {
            final BigInteger shardId = BigInteger.valueOf(s);
            steps.add(runStep("oracle_finalize_shard_" + s,
                    () -> blockchainService.finalizeOracleShardInfo(target, shardId)));
        }

        for (int s = 0; s < n; s++) {
            final BigInteger shardId = BigInteger.valueOf(s);
            steps.add(runStep("sync_shard_" + s,
                    () -> blockchainService.syncShardInfo(target, shardId)));
        }

        steps.add(runStep("adjust_resource_weights",
                () -> blockchainService.adjustResourceWeightsInfo()));

        for (int s = 0; s < n; s++) {
            final BigInteger shardId = BigInteger.valueOf(s);
            steps.add(runStep("publish_privatized_load_" + s,
                    () -> blockchainService.publishPrivatizedLoadInfo(target, shardId)));
        }

        steps.add(runStep("record_batch", this::reportCrossShardToChain));
        steps.add(runStep("controller_step",
                () -> blockchainService.controllerStepInfo(target)));

        epochGasRecorder.record(currentEpoch.intValue(), steps);
        epochGasRecorder.writeJson();

        Long nextEpoch = coordinatorService.advanceEpoch();
        log.info("Epoch 推进完成：{} → {}", currentEpoch, nextEpoch);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private EpochGasStep runStep(String name, Supplier<BlockchainService.TxInfo> supplier) {
        long start = System.currentTimeMillis();
        try {
            BlockchainService.TxInfo info = supplier.get();
            return new EpochGasStep(name,
                    info == null ? null : info.gasUsed(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
             
            log.warn("Epoch 步骤 {} 失败（不影响 DB 推进）：{}", name, e.getMessage());
            return new EpochGasStep(name, null, System.currentTimeMillis() - start);
        }
    }

    private BlockchainService.TxInfo reportCrossShardToChain() {
        try {
            List<ChainTranscription> pending = chainTranscriptionService.lambdaQuery()
                    .and(w -> w.isNull(ChainTranscription::getRecordedOnChain)
                            .or().eq(ChainTranscription::getRecordedOnChain, false))
                    .list();
            if (pending == null || pending.isEmpty()) {
                return null;
            }
            long cross = pending.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getCrossShard()))
                    .count();
            BlockchainService.TxInfo info = blockchainService.recordCrossShardBatchInfo(
                    BigInteger.valueOf(cross), BigInteger.valueOf(pending.size()));
             
            chainTranscriptionService.markRecordedOnChain(
                    pending.stream().map(ChainTranscription::getRequestId).toList());
            log.info("跨片统计已聚合上报：cross={}, total={}, tx={}",
                    cross, pending.size(), info.txHash());
            return info;
        } catch (Exception e) {
             
            log.warn("跨片统计聚合上报失败（不影响主链路）：{}", e.getMessage());
            return null;
        }
    }

    private java.util.Optional<BigInteger> chainEpoch() {
        try {
            return java.util.Optional.of(blockchainService.currentEpoch());
        } catch (Exception e) {
            log.debug("链上 epoch 不可读（回退 DB epoch）：{}", e.getMessage());
            return java.util.Optional.empty();
        }
    }
}