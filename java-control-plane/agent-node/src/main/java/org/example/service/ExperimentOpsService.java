package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.dto.OnChainOpsRecord;
import org.example.dto.OpOutcome;
import org.example.entity.ChainRequest;
import org.example.enums.TxType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ExperimentOpsService {

    @Value("${agent.ops.target-rate:5.0}")
    private double targetRate;

    @Value("${agent.ops.duration-sec:30}")
    private int durationSec;

    /** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
    @Value("${agent.ops.output-dir:outputs/onchain}")
    private String outputDir;

    @Value("${agent.ops.netem-enabled:true}")
    private boolean netemEnabled;

    @Value("${agent.ops.intra-ratio:0.7}")
    private double intraRatio;

    @Value("${agent.ops.run-id:}")
    private String configuredRunId;

    @Value("${agent.ops.netem-delay-ms:50}")
    private int netemDelayMs;

    @Value("${agent.ops.netem-jitter-ms:10}")
    private int netemJitterMs;

    @Value("${agent.ops.shard-addresses-file:}")
    private String shardAddressesFile;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BlockchainService blockchainService;

    private final CopyOnWriteArrayList<OnChainOpsRecord> ops = new CopyOnWriteArrayList<>();

    private final AtomicReference<ScheduledExecutorService> scheduler = new AtomicReference<>();

    private final AtomicLong seq = new AtomicLong(0);
    private final SecureRandom random = new SecureRandom();

    private volatile boolean running = false;
    private volatile long startMs = 0;
    private volatile String runId;

    private volatile Map<String, Integer> shardPool = Map.of();
    private volatile boolean poolReady = false;

    public synchronized Map<String, Object> start() {
        if (running) {
            return status();
        }
        if (!netemEnabled) {
            throw new IllegalStateException(
                    "agent.ops.netem-enabled=false：链上实验要求 netem 已启用（链路时延注入），"
                            + "拒绝启动。请先执行 chain/scripts/03-netem.sh 注入时延，"
                            + "并将 agent.ops.netem-enabled 置为 true 后重试。");
        }
        ops.clear();
        seq.set(0);
        running = true;
        startMs = System.currentTimeMillis();
        runId = (configuredRunId == null || configuredRunId.isBlank())
                ? "ops-" + java.time.Instant.now().toString().replace(":", "").replace("-", "").substring(0, 15)
                : configuredRunId;

        refreshPool();

        long periodMs = Math.max(1, Math.round(1000.0 / Math.max(0.5, targetRate)));
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "experiment-ops"));
        scheduler.set(exec);
        exec.scheduleAtFixedRate(this::fireOne, 0, periodMs, TimeUnit.MILLISECONDS);
         
        exec.schedule(this::stop, durationSec, TimeUnit.SECONDS);
        log.info("实验操作批量生成启动：runId={}, targetRate={}/s, durationSec={}s, "
                        + "intraRatio={}, netemEnabled={}, outputDir={}",
                runId, targetRate, durationSec, intraRatio, netemEnabled, outputDir);
        return status();
    }

    public synchronized Map<String, Object> stop() {
        ScheduledExecutorService exec = scheduler.getAndSet(null);
        if (exec != null) {
            exec.shutdownNow();
        }
        if (!running) {
            return status();
        }
        running = false;
        writeJson();
        return status();
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("running", running);
        m.put("run_id", runId);
        m.put("elapsed_sec", running
                ? Math.round((System.currentTimeMillis() - startMs) / 100.0) / 10.0 : 0.0);
        long committed = ops.stream().filter(r -> "committed".equals(r.getStatus())).count();
        long intra = ops.stream().filter(r -> "intra".equals(r.getType())).count();
        m.put("submitted", ops.size());
        m.put("committed", committed);
        m.put("aborted", ops.size() - committed);
        m.put("intra", intra);
        m.put("cross", ops.size() - intra);
        return m;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private void fireOne() {
        if (!running) {
            return;
        }
        long t0 = System.currentTimeMillis();
        long s = seq.incrementAndGet();
        try {
            String[] pair = pickPair();
            ChainRequest req = new ChainRequest();
            req.setRequestId("exp-" + runId + "-" + s);
            req.setTxType(TxType.TRANSFER);    
            req.setSenderAddress(pair[0]);
            req.setReceiverAddress(pair[1]);
            req.setAssetId("asset-" + s);
            req.setAmount(new BigDecimal("100"));
            req.setTimestamp(t0);

            OpOutcome out = transactionService.submitForExperiment(req, s);
            String type = out.isCrossShard() ? "cross" : "intra";
            boolean committed = out.getError() == null;
            ops.add(OnChainOpsRecord.builder()
                    .seq(s)
                    .type(type)
                    .status(committed ? "committed" : "aborted")
                    .submitMs(out.getSubmitMs())
                    .confirmMs(out.getConfirmMs())
                    .gasUsed(out.getGasUsed() == null ? 0 : out.getGasUsed().longValue())
                    .txHash(out.getTxHash())
                    .blockNumber(out.getBlockNumber())
                    .srcShard(out.getSenderShardId() == null ? -1 : out.getSenderShardId())
                    .dstShard(out.getReceiverShardId() == null ? -1 : out.getReceiverShardId())
                    .build());
            log.debug("实验操作 #{} 完成：type={}, latency={}ms", s, type,
                    out.getConfirmMs() - out.getSubmitMs());
        } catch (Exception e) {
            log.warn("实验操作 #{} 失败（记 aborted）：{}", s, e.getMessage());
            ops.add(OnChainOpsRecord.builder()
                    .seq(s)
                    .type("unknown")
                    .status("aborted")
                    .submitMs(t0)
                    .confirmMs(System.currentTimeMillis())
                    .gasUsed(0)
                    .txHash(null)
                    .srcShard(-1)
                    .dstShard(-1)
                    .build());
        }
    }

    private String[] pickPair() {
        boolean wantIntra = ThreadLocalRandom.current().nextDouble() < intraRatio;
        if (poolReady && !shardPool.isEmpty()) {
            List<String> addrs = new ArrayList<>(shardPool.keySet());
            String a = addrs.get(ThreadLocalRandom.current().nextInt(addrs.size()));
            int ga = shardPool.get(a);
            for (int i = 0; i < 40; i++) {
                String b = addrs.get(ThreadLocalRandom.current().nextInt(addrs.size()));
                int gb = shardPool.get(b);
                if (wantIntra == (ga == gb)) {
                    return new String[]{a, b};
                }
            }
        }
        return new String[]{randomAddr(), randomAddr()};
    }

    private void refreshPool() {
        Map<String, Integer> pool = new LinkedHashMap<>();
        boolean fromFile = shardAddressesFile != null && !shardAddressesFile.isBlank();
        if (fromFile) {
            Path f = Paths.get(shardAddressesFile);
            if (Files.exists(f)) {
                try {
                    JsonNode root = new ObjectMapper().readTree(
                            Files.readString(f, StandardCharsets.UTF_8));
                    for (JsonNode shard : root.path("shards")) {
                        int sid = shard.path("shard").asInt();
                        for (JsonNode addr : shard.path("addresses")) {
                            pool.put(addr.asText(), sid);
                        }
                    }
                    log.info("从注册地址清单加载 {} 个地址（{}）", pool.size(), shardAddressesFile);
                } catch (Exception e) {
                    log.warn("注册地址清单读取失败（退化为随机地址）：{}", e.getMessage());
                    fromFile = false;
                    pool.clear();
                }
            } else {
                log.warn("注册地址清单不存在（{}），退化为随机地址", shardAddressesFile);
                fromFile = false;
            }
        }
        if (!fromFile) {
            try {
                for (int i = 0; i < 64; i++) {
                    String a = randomAddr();
                    try {
                        pool.put(a, blockchainService.getOptimalShard(a).intValue());
                    } catch (Exception ignored) {
                         
                    }
                }
            } catch (Exception e) {
                log.warn("路由地址池预查询失败（退化为随机地址，type 以链上判定为准）：{}", e.getMessage());
            }
        } else {
             
            Map<String, Integer> confirmed = new LinkedHashMap<>();
            for (String a : pool.keySet()) {
                try {
                    confirmed.put(a, blockchainService.getOptimalShard(a).intValue());
                } catch (Exception ignored) {
                    confirmed.put(a, pool.get(a));
                }
            }
            pool = confirmed;
        }
        shardPool = pool;
        poolReady = !pool.isEmpty();
        log.info("路由地址池就绪：{} 个地址（{}）", pool.size(), fromFile ? "注册清单+链上确认" : "随机地址");
    }

    private String randomAddr() {
        byte[] b = new byte[20];
        random.nextBytes(b);
        return "0x" + HexFormat.of().formatHex(b);
    }

    private void writeJson() {
        try {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);
            long durationMs = Math.max(0, System.currentTimeMillis() - startMs);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("run_id", runId);
            Map<String, Object> netem = new LinkedHashMap<>();
            netem.put("enabled", netemEnabled);
            netem.put("delay_ms", netemDelayMs);
            netem.put("jitter_ms", netemJitterMs);
            root.put("netem", netem);
            root.put("duration_sec", Math.round(durationMs / 100.0) / 10.0);
            root.put("summary", OnChainOpsReportService.summary(ops));
             
            long committedIntra = ops.stream()
                    .filter(r -> "intra".equals(r.getType()) && "committed".equals(r.getStatus()))
                    .count();
            long committedCross = ops.stream()
                    .filter(r -> "cross".equals(r.getType()) && "committed".equals(r.getStatus()))
                    .count();
            root.put("intra_op_count", committedIntra);
            root.put("cross_op_count", committedCross);

            List<Map<String, Object>> opList = new ArrayList<>();
            for (OnChainOpsRecord r : ops) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("seq", r.getSeq());
                o.put("type", r.getType());
                o.put("status", r.getStatus());
                o.put("submit_ms", r.getSubmitMs());
                o.put("confirm_ms", r.getConfirmMs());
                o.put("gas_used", r.getGasUsed());
                o.put("tx_hash", r.getTxHash());
                o.put("blockNumber", r.getBlockNumber());
                o.put("src_shard", r.getSrcShard());
                o.put("dst_shard", r.getDstShard());
                opList.add(o);
            }
            root.put("operations", opList);

            Path out = dir.resolve("onchain_ops.json");
            Files.writeString(out,
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root),
                    StandardCharsets.UTF_8);
            log.info("onchain_ops.json 已写入：{}（operations={}）", out.toAbsolutePath(), opList.size());
        } catch (Exception e) {
            log.error("onchain_ops.json 写入失败：{}", e.getMessage(), e);
        }
    }
}
