package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.config.ShardLifecycle;
import org.example.dto.SystemMetrics;
import org.example.entity.Shard;
import org.example.util.SystemMetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class LoadMetricsReporter {

    @Value("${shard.metrics.enabled}")
    private boolean enabled;

    @Value("${shard.metrics.interval-ms}")
    private long intervalMs;

    @Value("${shard.metrics.lie-enabled:false}")
    private boolean lieEnabled;

    @Value("${shard.metrics.lie-mode:lower}")
    private String lieMode;

    @Value("${shard.metrics.lie-ratio:1.0}")
    private double lieRatio;

    @Value("${shard.metrics.output-dir:outputs/onchain}")
    private String outputDir;

    @Autowired
    private ShardService shardService;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ShardSelfService shardSelfService;

    @Autowired
    private SystemMetricsCollector metricsCollector;

    @Autowired
    private TpsTracker tpsTracker;

    @Autowired
    private ShardLifecycle shardLifecycle;

    private volatile SystemMetrics lastMetrics;

    private final AtomicLong lieCounter = new AtomicLong();

    @Scheduled(fixedDelayString = "${shard.metrics.interval-ms}")
    public void report() {
         
        if (!enabled) {
            return;
        }
         
        if (!shardLifecycle.isReady()) {
            return;
        }
         
        Integer shardId = shardSelfService.shardId();
        if (shardId == null) {
            log.debug("本分片尚未注册（shard 表无 ip+port 记录），跳过本轮负载上报");
            return;
        }

        SystemMetrics metrics = metricsCollector.collect();
        lastMetrics = metrics;
        long tps = (intervalMs > 0)
                ? Math.round(tpsTracker.resetAndGet() * 1000.0 / intervalMs)
                : 0;

        boolean updated = writeLoadMetrics(shardId, metrics, tps);
        if (!updated) {

            shardSelfService.invalidate();
            Integer refreshedId = shardSelfService.shardId();
            if (refreshedId != null) {
                log.warn("shardId={} 落库失败，刷新身份后重试：新 shardId={}", shardId, refreshedId);
                updated = writeLoadMetrics(refreshedId, metrics, tps);
            }
            if (!updated) {
                log.error("负载指标落库失败（shardId={} 不存在且无法刷新），本节点可能已从分片池移除，本轮上报终止",
                        shardId);
                return;
            }
        }
        log.info("负载指标上报：shardId={}, cpu={}%, mem={}%, disk={}%, net={}%, tps={}, updated={}",
                shardId, pct(metrics.getCpuUsage()), pct(metrics.getMemoryUsage()),
                pct(metrics.getDiskIoLoad()), pct(metrics.getNetworkBandwidth()), tps, updated);

        reportToChain(shardId, metrics);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private boolean writeLoadMetrics(Integer shardId, SystemMetrics metrics, long tps) {
        return shardService.updateLoadMetrics(Shard.builder()
                .shardId(shardId)
                .cpuUsage(metrics.getCpuUsage())
                .memoryUsage(metrics.getMemoryUsage())
                .diskIoLoad(metrics.getDiskIoLoad())
                .networkBandwidth(metrics.getNetworkBandwidth())
                .currentTps(tps)
                .build());
    }

    public SystemMetrics lastMetrics() {
        return lastMetrics;
    }

    private void reportToChain(Integer shardId, SystemMetrics metrics) {
        try {
            BigInteger epoch = currentEpoch();
            long cpuHonest = permille(metrics.getCpuUsage());
            long memHonest = permille(metrics.getMemoryUsage());
            long diskHonest = permille(metrics.getDiskIoLoad());
            long netHonest = permille(metrics.getNetworkBandwidth());

            boolean lie = shouldLie();
            long cpu = lie ? lieValue() : cpuHonest;
            long mem = lie ? lieValue() : memHonest;
            long disk = lie ? lieValue() : diskHonest;
            long net = lie ? lieValue() : netHonest;

            String txHash = blockchainService.submitReading(epoch, BigInteger.valueOf(shardId),
                    BigInteger.valueOf(cpu), BigInteger.valueOf(mem),
                    BigInteger.valueOf(disk), BigInteger.valueOf(net), BigInteger.ZERO);
            if (lie) {
                log.info("拜占庭谎报：shardId={}, epoch={}, 诚实=[{}, {}, {}, {}] → 上报=[{}, {}, {}, {}], tx={}",
                        shardId, epoch, cpuHonest, memHonest, diskHonest, netHonest,
                        cpu, mem, disk, net, txHash);
            } else {
                log.debug("链上负载上报成功：shardId={}, epoch={}, tx={}", shardId, epoch, txHash);
            }
             
            writeByzantineFragment(shardId, cpuHonest, cpu);
        } catch (Exception e) {
             
            log.warn("链上负载上报失败（不影响本地落库）：{}", e.getMessage());
        }
    }

    private boolean shouldLie() {
        if (!lieEnabled) {
            return false;
        }
        int period = (int) Math.max(1, Math.round(1.0 / Math.max(0.0001, lieRatio)));
        return lieCounter.getAndIncrement() % period == 0;
    }

    private long lieValue() {
        return "upper".equalsIgnoreCase(lieMode) ? 10000L : 100L;
    }

    private void writeByzantineFragment(Integer shardId, long honestCpu, long observedCpu) {
        try {
            File dir = new File(outputDir);
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("无法创建输出目录：{}", outputDir);
                return;
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("shard", shardId);
            root.put("honest_median", honestCpu);
            root.put("observed_median", observedCpu);
            File out = new File(dir, "onchain_byzantine.shard-" + shardId + ".json");
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, root);
        } catch (Exception e) {
            log.warn("写 onchain_byzantine 片段失败：{}", e.getMessage());
        }
    }

    private BigInteger currentEpoch() {
        try {
            return blockchainService.currentEpoch();
        } catch (Exception e) {
            Long epoch = coordinatorService.getCurrentEpoch();
            return epoch == null ? BigInteger.ONE : BigInteger.valueOf(epoch);
        }
    }

    private static long permille(Double value) {
        double v = (value == null) ? 0.0 : value;
        return (long) (Math.clamp(v, 0.0, 1.0) * 10000);
    }

    private static String pct(Double value) {
        double v = (value == null) ? 0.0 : value;
        return String.format("%.1f", v * 100);
    }
}