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
import org.example.entity.ChainTranscription;
import org.example.entity.Coordinator;
import org.example.entity.Task;
import org.example.enums.ReceiptStatus;
import org.example.enums.TaskStatus;
import org.example.mapper.ChainTranscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExperimentDataService {

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ChainReceiptService chainReceiptService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ChainTranscriptionMapper chainTranscriptionMapper;

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();

        Coordinator coordinator = coordinatorService.getOne(new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
        if (coordinator != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("coordinatorId", coordinator.getCoordinatorId());
            c.put("epoch", coordinator.getCurrentEpoch());
            c.put("globalState", coordinator.getGlobalState());
            c.put("totalShards", coordinator.getTotalShards());
            c.put("epochDuration", coordinator.getEpochDuration());
            result.put("coordinator", c);
        } else {
            result.put("coordinator", null);
        }

        result.put("shards", shardService.listAll());

        Map<String, Long> tasks = new LinkedHashMap<>();
        tasks.put("PENDING", taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.PENDING)));
        tasks.put("PROCESSING", taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.PROCESSING)));
        tasks.put("COMPLETED", taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.COMPLETED)));
        tasks.put("FAILED", taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.FAILED)));
        result.put("tasks", tasks);
        return result;
    }

    public ExperimentStats experimentStats() {
        long total = chainReceiptService.count();
        long success = chainReceiptService.countByStatus(ReceiptStatus.SUCCESS);
        long failed = chainReceiptService.countByStatus(ReceiptStatus.FAILED);
        double rate = total == 0 ? 0.0 : success * 100.0 / total;
        ReceiptStats receipt = new ReceiptStats(total, success, failed, rate);
        return new ExperimentStats(receipt, transcriptionStats(),
                shardService.listAll().size(), agentService.list().size());
    }

    public Map<String, Object> stats() {
        ExperimentStats s = experimentStats();
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("total", s.getReceipt().getTotal());
        receipt.put("success", s.getReceipt().getSuccess());
        receipt.put("failed", s.getReceipt().getFailed());
        receipt.put("successRate", s.getReceipt().getSuccessRate());
        result.put("receipt", receipt);

        result.put("transcription", transcriptionToMap(s.getTranscription()));

        result.put("shardCount", s.getShardCount());
        result.put("agentCount", s.getAgentCount());
        return result;
    }

    private TranscriptionStats transcriptionStats() {
        List<ChainTranscription> list = chainTranscriptionMapper.selectList(null);
        long total = list.size();
        if (total == 0) {
            return new TranscriptionStats();
        }

        long crossCount = list.stream().filter(x -> Boolean.TRUE.equals(x.getCrossShard())).count();
        CrossShardStats cross = new CrossShardStats(crossCount, roundRate(crossCount, total));

        Map<Integer, Long> byShard = list.stream().filter(x -> x.getTargetShardId() != null)
                .collect(Collectors.groupingBy(ChainTranscription::getTargetShardId, Collectors.counting()));

        Map<Long, Long> byEpoch = list.stream().filter(x -> x.getBlockNumber() != null)
                .collect(Collectors.groupingBy(ChainTranscription::getBlockNumber, Collectors.counting()));

        Map<String, Long> byTxType = list.stream().filter(x -> x.getTxType() != null)
                .collect(Collectors.groupingBy(x -> x.getTxType().name(), Collectors.counting()));

        Map<String, Long> bySource = list.stream().filter(x -> x.getSource() != null)
                .collect(Collectors.groupingBy(ChainTranscription::getSource, Collectors.counting()));

        long recorded = list.stream().filter(x -> Boolean.TRUE.equals(x.getRecordedOnChain())).count();
        OnChainStats onChain = new OnChainStats(recorded, roundRate(recorded, total));

        long totalGas = list.stream().mapToLong(x -> x.getGasUsed() == null ? 0 : x.getGasUsed()).sum();
        GasStats gas = new GasStats(totalGas, round2(totalGas * 1.0 / total));

        List<ChainTranscription> withLatency = list.stream()
                .filter(x -> x.getTotalLatency() != null).toList();
        double avgLatency = withLatency.stream().mapToLong(ChainTranscription::getTotalLatency).average().orElse(0.0);
        LatencyStats latency = new LatencyStats(withLatency.size(), round2(avgLatency));

        return new TranscriptionStats(total, cross, byShard, byEpoch, byTxType, bySource, onChain, gas, latency);
    }

    private Map<String, Object> transcriptionToMap(TranscriptionStats t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", t.getTotal());
        if (t.getCrossShard() != null) {
            Map<String, Object> cross = new LinkedHashMap<>();
            cross.put("count", t.getCrossShard().getCount());
            cross.put("rate", t.getCrossShard().getRate());
            m.put("crossShard", cross);
        }
        if (t.getByShard() != null) {
            m.put("byShard", t.getByShard());
        }
        if (t.getByEpoch() != null) {
            m.put("byEpoch", t.getByEpoch());
        }
        if (t.getByTxType() != null) {
            m.put("byTxType", t.getByTxType());
        }
        if (t.getBySource() != null) {
            m.put("bySource", t.getBySource());
        }
        if (t.getRecordedOnChain() != null) {
            Map<String, Object> onChain = new LinkedHashMap<>();
            onChain.put("count", t.getRecordedOnChain().getCount());
            onChain.put("rate", t.getRecordedOnChain().getRate());
            m.put("recordedOnChain", onChain);
        }
        if (t.getGas() != null) {
            Map<String, Object> gas = new LinkedHashMap<>();
            gas.put("total", t.getGas().getTotal());
            gas.put("avg", t.getGas().getAvg());
            m.put("gas", gas);
        }
        if (t.getLatency() != null) {
            Map<String, Object> latency = new LinkedHashMap<>();
            latency.put("count", t.getLatency().getCount());
            latency.put("avg", t.getLatency().getAvg());
            m.put("latency", latency);
        }
        return m;
    }

    private static double roundRate(long part, long total) {
        return total == 0 ? 0.0 : Math.round(part * 10000.0 / total) / 100.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}