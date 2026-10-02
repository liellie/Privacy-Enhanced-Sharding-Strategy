package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ExperimentStats;
import org.example.dto.ExperimentStats.CrossShardStats;
import org.example.dto.ExperimentStats.GasStats;
import org.example.dto.ExperimentStats.LatencyStats;
import org.example.dto.ExperimentStats.OnChainStats;
import org.example.dto.ExperimentStats.ReceiptStats;
import org.example.dto.ExperimentStats.TranscriptionStats;
import org.example.entity.Agent;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainTranscription;
import org.example.entity.Coordinator;
import org.example.entity.Shard;
import org.example.entity.Task;
import org.example.enums.TaskStatus;
import org.example.mapper.ChainReceiptMapper;
import org.example.mapper.ChainTranscriptionMapper;
import org.example.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExperimentReportService {

    private static final String REPORT_DIR = "reports";

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_FMT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ChainReceiptMapper chainReceiptMapper;

    @Autowired
    private ChainTranscriptionMapper chainTranscriptionMapper;

    @Autowired
    private ExperimentDataService experimentDataService;

    public String generateReport(String endReason) {
        try {
            Path dir = Paths.get(REPORT_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("experiment-report-" + FILE_FMT.format(new Date()) + ".md");
            Files.writeString(file, buildMarkdown(endReason), StandardCharsets.UTF_8);
            String abs = file.toAbsolutePath().toString();
            log.info("实验报告已生成：{}", abs);
            return abs;
        } catch (IOException e) {
            log.error("实验报告生成失败：{}", e.getMessage(), e);
            return null;
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private String buildMarkdown(String endReason) {
        StringBuilder md = new StringBuilder();
        Coordinator coordinator = findCoordinator();
        ExperimentStats stats = experimentDataService.experimentStats();

        md.append("# 分片链（ShardChain）实验报告\n\n");
        md.append("- 生成时间：").append(time(System.currentTimeMillis())).append('\n');
        md.append("- 结束原因：").append(endReason).append('\n');
        if (coordinator != null) {
            md.append("- 协调者 ID：").append(coordinator.getCoordinatorId()).append('\n');
            md.append("- 当前 Epoch：").append(coordinator.getCurrentEpoch()).append('\n');
            md.append("- Epoch 时长：").append(coordinator.getEpochDuration()).append(" ms\n");
            md.append("- 分片总数：").append(coordinator.getTotalShards()).append('\n');
        }
        md.append('\n');

        appendOverview(md, coordinator, stats);
        appendReceipt(md, stats.getReceipt());
        appendTranscription(md, stats.getTranscription());
        appendTasks(md);
        appendShards(md);
        appendAgents(md);
        appendRecentReceipts(md);
        return md.toString();
    }

    private void appendOverview(StringBuilder md, Coordinator c, ExperimentStats stats) {
        md.append("## 一、实验概览\n\n");
        md.append("| 指标 | 值 |\n|---|---|\n");
        md.append("| 分片数量 | ").append(stats.getShardCount()).append(" |\n");
        md.append("| Agent 数量 | ").append(stats.getAgentCount()).append(" |\n");
        md.append("| 交易请求总量 | ").append(chainReceiptMapper.selectCount(null)).append(" |\n");
        md.append("| 回执总量 | ").append(stats.getReceipt().getTotal()).append(" |\n");
        md.append("| 成功率 | ").append(String.format("%.2f%%", stats.getReceipt().getSuccessRate())).append(" |\n");
        if (c != null && c.getEpochStartTime() != null) {
            md.append("| 最近 Epoch 开始 | ").append(time(c.getEpochStartTime())).append(" |\n");
        }
        md.append('\n');
    }

    private void appendReceipt(StringBuilder md, ReceiptStats r) {
        md.append("## 二、回执统计\n\n");
        md.append("| 指标 | 值 |\n|---|---|\n");
        md.append("| 总量 | ").append(r.getTotal()).append(" |\n");
        md.append("| 成功 | ").append(r.getSuccess()).append(" |\n");
        md.append("| 失败 | ").append(r.getFailed()).append(" |\n");
        md.append("| 成功率 | ").append(String.format("%.2f%%", r.getSuccessRate())).append(" |\n");
        md.append('\n');
    }

    private void appendTranscription(StringBuilder md, TranscriptionStats t) {
        md.append("## 三、链上转录统计\n\n");
        md.append("| 维度 | 统计 |\n|---|---|\n");
        md.append("| 转录总量 | ").append(t.getTotal()).append(" |\n");
        if (t.getCrossShard() != null) {
            CrossShardStats cross = t.getCrossShard();
            md.append("| 跨片 | ").append(cross.getCount()).append(" 笔（")
                    .append(String.format("%.2f%%", cross.getRate())).append("） |\n");
        }
        if (t.getByShard() != null && !t.getByShard().isEmpty()) {
            md.append("| 按分片 | ").append(mapToString(t.getByShard())).append(" |\n");
        }
        if (t.getByEpoch() != null && !t.getByEpoch().isEmpty()) {
            md.append("| 按Epoch | ").append(mapToString(t.getByEpoch())).append(" |\n");
        }
        if (t.getByTxType() != null && !t.getByTxType().isEmpty()) {
            md.append("| 按类型 | ").append(mapToString(t.getByTxType())).append(" |\n");
        }
        if (t.getBySource() != null && !t.getBySource().isEmpty()) {
            md.append("| 按来源 | ").append(mapToString(t.getBySource())).append(" |\n");
        }
        if (t.getRecordedOnChain() != null) {
            OnChainStats onChain = t.getRecordedOnChain();
            md.append("| 链上登记 | ").append(onChain.getCount()).append(" 笔（")
                    .append(String.format("%.2f%%", onChain.getRate())).append("） |\n");
        }
        if (t.getGas() != null) {
            GasStats gas = t.getGas();
            md.append("| Gas | 总 ").append(gas.getTotal()).append(" / 均 ").append(gas.getAvg()).append(" |\n");
        }
        if (t.getLatency() != null) {
            LatencyStats latency = t.getLatency();
            md.append("| 耗时 | ").append(latency.getCount()).append(" 笔，平均 ")
                    .append(latency.getAvg()).append(" ms |\n");
             
            List<Long> lats = chainTranscriptionMapper.selectList(null).stream()
                    .map(ChainTranscription::getTotalLatency)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.naturalOrder())
                    .toList();
            if (!lats.isEmpty()) {
                md.append("| 耗时 P50 | ").append(percentile(lats, 50)).append(" ms |\n");
                md.append("| 耗时 P95 | ").append(percentile(lats, 95)).append(" ms |\n");
                md.append("| 耗时 P99 | ").append(percentile(lats, 99)).append(" ms |\n");
            }
        }
        md.append('\n');
    }

    private void appendTasks(StringBuilder md) {
        List<Task> tasks = taskMapper.selectList(null);
        Map<TaskStatus, Long> byStatus = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        md.append("## 四、任务统计\n\n");
        if (tasks.isEmpty()) {
            md.append("无任务记录\n\n");
            return;
        }
        md.append("| 状态 | 数量 |\n|---|---|\n");
        for (TaskStatus status : TaskStatus.values()) {
            long count = byStatus.getOrDefault(status, 0L);
            md.append("| ").append(status).append(" | ").append(count).append(" |\n");
        }
        md.append('\n');
    }

    private void appendShards(StringBuilder md) {
        List<Shard> shards = shardService.listAll();
        md.append("## 五、分片负载明细\n\n");
        if (shards.isEmpty()) {
            md.append("无分片记录\n\n");
            return;
        }
        md.append("| 分片ID | IP | 端口 | CPU% | 内存% | 磁盘% | 网络% | TPS |\n|---|---|---|---|---|---|---|---|\n");
        for (Shard s : shards) {
            md.append("| ").append(s.getShardId())
                    .append(" | ").append(s.getIpAddress())
                    .append(" | ").append(s.getPort())
                    .append(" | ").append(nvl(s.getCpuUsage()))
                    .append(" | ").append(nvl(s.getMemoryUsage()))
                    .append(" | ").append(nvl(s.getDiskIoLoad()))
                    .append(" | ").append(nvl(s.getNetworkBandwidth()))
                    .append(" | ").append(nvl(s.getCurrentTps()))
                    .append(" |\n");
        }
        md.append('\n');
    }

    private void appendAgents(StringBuilder md) {
        List<Agent> agents = agentService.list();
        md.append("## 六、Agent 明细\n\n");
        if (agents.isEmpty()) {
            md.append("无 Agent 记录\n\n");
            return;
        }
        md.append("| Agent ID | 组织 | 归属分片 | 状态 |\n|---|---|---|---|\n");
        for (Agent a : agents) {
            md.append("| ").append(a.getAgentId())
                    .append(" | ").append(a.getOrganizationId())
                    .append(" | ").append(a.getCurrentShardId())
                    .append(" | ").append(a.getStatus())
                    .append(" |\n");
        }
        md.append('\n');
    }

    private void appendRecentReceipts(StringBuilder md) {
        List<ChainReceipt> receipts = chainReceiptMapper.selectList(null);
        md.append("## 七、最近回执明细\n\n");
        if (receipts.isEmpty()) {
            md.append("无回执记录\n\n");
            return;
        }
        receipts.sort((a, b) -> Long.compare(
                b.getTimestamp() == null ? 0 : b.getTimestamp(),
                a.getTimestamp() == null ? 0 : a.getTimestamp()));
        List<ChainReceipt> recent = receipts.stream().limit(10).toList();
        md.append("| 请求ID | 状态 | Epoch | Gas | 时间 |\n|---|---|---|---|---|\n");
        for (ChainReceipt r : recent) {
            md.append("| ").append(shortId(r.getRequestId()))
                    .append(" | ").append(r.getStatus())
                    .append(" | ").append(r.getBlockNumber())
                    .append(" | ").append(r.getGasUsed())
                    .append(" | ").append(time(r.getTimestamp()))
                    .append(" |\n");
        }
        md.append('\n');
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private Coordinator findCoordinator() {
        return coordinatorService.getOne(new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
    }

    private static String mapToString(Map<?, ?> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String nvl(Object o) {
        return o == null ? "-" : o.toString();
    }

    private static Long percentile(List<Long> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) {
            return null;
        }
        int idx = Math.max(0, Math.min(sorted.size() - 1,
                (int) Math.ceil(p / 100.0 * sorted.size()) - 1));
        return sorted.get(idx);
    }

    private static String time(Long ts) {
        return ts == null ? "-" : TIME_FMT.format(new Date(ts));
    }

    private static String shortId(String id) {
        return id == null ? "-" : id.length() > 8 ? id.substring(0, 8) : id;
    }
}
