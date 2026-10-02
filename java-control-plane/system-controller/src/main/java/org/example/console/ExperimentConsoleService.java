package org.example.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ExperimentStats;
import org.example.dto.ExperimentStats.ReceiptStats;
import org.example.dto.ExperimentStats.TranscriptionStats;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainRequest;
import org.example.entity.Coordinator;
import org.example.entity.Task;
import org.example.enums.CoordinatorState;
import org.example.enums.TaskStatus;
import org.example.mapper.ChainReceiptMapper;
import org.example.mapper.ChainRequestMapper;
import org.example.mapper.TaskMapper;
import org.example.service.AgentService;
import org.example.service.CoordinatorService;
import org.example.service.ExperimentDataService;
import org.example.service.ExperimentFinishService;
import org.example.service.NodeCommandService;
import org.example.service.ShardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExperimentConsoleService {

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private NodeCommandService nodeCommandService;

    @Autowired
    private ExperimentDataService experimentDataService;

    @Autowired
    private ExperimentFinishService experimentFinishService;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ChainRequestMapper chainRequestMapper;

    @Autowired
    private ChainReceiptMapper chainReceiptMapper;

    @Autowired
    private TaskMapper taskMapper;

    public void init() {
        divider("初始化所有节点");
        nodeCommandService.initAll();
        println("初始化指令已下发完成");
    }

    public void start() {
        divider("开始实验");
        Coordinator coordinator = findCoordinator();
        if (coordinator == null) {
            println("[WARN] 协调者尚未初始化：请先执行 init 命令（确认 coordinator / shard-node / agent-node 已启动并注册到 Nacos）");
            return;
        }
         
        nodeCommandService.startAll();
        coordinatorService.updateGlobalState(CoordinatorState.RUNNING);
         
        experimentFinishService.resetDetection();
        int shardCount = shardService.listAll().size();
        int agentCount = agentService.list().size();
        println("实验已开始（协调者 RUNNING，自动调度已启用）");
        println("节点就绪：分片 " + shardCount + " 个，Agent " + agentCount + " 个");
    }

    public void stop() {
        divider("结束实验");
        Coordinator coordinator = findCoordinator();
        if (coordinator == null) {
            println("[WARN] 协调者未初始化，无可结束的实验");
            return;
        }
         
        nodeCommandService.stopAll();
        coordinatorService.updateGlobalState(CoordinatorState.PAUSED);
        println("实验已结束（协调者 PAUSED，自动 epoch 推进与再平衡检查已暂停）");
    }

    public void shutdown() {
        divider("关闭所有节点");
        nodeCommandService.shutdownAll();
        println("关闭指令已下发完成");
    }

    public void finish() {
        divider("结束实验并生成报告");
        String reportPath = experimentFinishService.finish("手动结束（finish 命令）");
        if (reportPath == null) {
            println("[WARN] 结束失败：协调者未初始化，或实验已处于 FINISHED 状态");
            return;
        }
        println("实验已结束（协调者 FINISHED，定时任务已关闭）");
        println("实验报告已生成：" + reportPath);
    }

    public void epoch() {
        Coordinator c = findCoordinator();
        if (c == null) {
            println("协调者未初始化，请先执行 init 或 start");
            return;
        }
        divider("当前 Epoch");
        println("协调者 ID  : " + c.getCoordinatorId());
        println("当前 Epoch : " + c.getCurrentEpoch());
        println("全局状态   : " + c.getGlobalState());
        println("Epoch 开始 : " + time(c.getEpochStartTime()));
        println("Epoch 时长 : " + c.getEpochDuration() + " ms");
        println("分片总数   : " + c.getTotalShards());
    }

    public void status() {
        divider("全局状态");
        printMap(experimentDataService.status(), 0);
    }

    public void stats() {
        divider("实验统计");
        printMap(experimentDataService.stats(), 0);
    }

    public void requests(String[] args) {
        int limit = args.length > 0 ? parseLimit(args[0]) : 20;
        List<ChainRequest> requests = chainRequestMapper.selectList(null);
        requests.sort(Comparator.comparing(ChainRequest::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        divider("最近 " + Math.min(limit, requests.size()) + " 笔交易请求（共 " + requests.size() + " 笔）");
        if (requests.isEmpty()) {
            println("暂无交易请求记录");
            return;
        }
        requests.stream().limit(limit).forEach(r -> println(
                "  " + shortId(r.getRequestId()) + " | " + r.getTxType()
                        + " | " + r.getSenderAddress() + " -> " + r.getReceiverAddress()
                        + " | amount=" + r.getAmount() + " | shard=" + r.getTargetShardId()
                        + " | " + time(r.getTimestamp())));
    }

    public void receipts(String[] args) {
        int limit = args.length > 0 ? parseLimit(args[0]) : 20;
        List<ChainReceipt> receipts = chainReceiptMapper.selectList(null);
        receipts.sort(Comparator.comparing(ChainReceipt::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        divider("最近 " + Math.min(limit, receipts.size()) + " 条回执��共 " + receipts.size() + " 条）");
        if (receipts.isEmpty()) {
            println("暂无回执记录");
            return;
        }
        receipts.stream().limit(limit).forEach(r -> println(
                "  " + shortId(r.getRequestId()) + " | " + r.getStatus()
                        + " | epoch=" + r.getBlockNumber() + " | gas=" + r.getGasUsed()
                        + " | " + time(r.getTimestamp())));
    }

    public void tasks(String[] args) {
        List<Task> tasks;
        if (args.length > 0 && !args[0].isBlank()) {
            TaskStatus status;
            try {
                status = TaskStatus.valueOf(args[0].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                println("非法任务状态: " + args[0] + "，可选 " + Arrays.toString(TaskStatus.values()));
                return;
            }
            tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().eq(Task::getStatus, status));
        } else {
            tasks = taskMapper.selectList(null);
        }
        tasks.sort(Comparator.comparing(Task::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        divider("任����列表（共 " + tasks.size() + " 条）");
        if (tasks.isEmpty()) {
            println("暂无任务记录");
            return;
        }
        tasks.forEach(t -> println(
                "  " + t.getTaskId() + " | " + t.getTaskType() + " | " + t.getStatus()
                        + " | " + t.getSourceShardId() + " -> " + t.getTargetShardId()
                        + " | " + time(t.getCreateTime())));
    }

    public void results(String[] args) {
        int limit = args.length > 0 ? parseLimit(args[0]) : 10;
        divider("实验结果");

        ExperimentStats stats = experimentDataService.experimentStats();
        println("【回执统计】");
        ReceiptStats receipt = stats.getReceipt();
        println("  总量     : " + receipt.getTotal());
        println("  成功     : " + receipt.getSuccess());
        println("  失败     : " + receipt.getFailed());
        println("  成功率   : " + String.format("%.2f%%", receipt.getSuccessRate()));

        TranscriptionStats t = stats.getTranscription();
        println("【链上转录】");
        println("  转录总量 : " + t.getTotal());
        if (t.getCrossShard() != null) {
            println("  跨片     : " + t.getCrossShard().getCount() + " 笔（"
                    + String.format("%.2f%%", t.getCrossShard().getRate()) + "）");
        }
        if (t.getByShard() != null) {
            println("  按分片   : " + t.getByShard());
        }
        if (t.getByEpoch() != null) {
            println("  按Epoch  : " + t.getByEpoch());
        }
        if (t.getByTxType() != null) {
            println("  按类型   : " + t.getByTxType());
        }
        if (t.getBySource() != null) {
            println("  按来源   : " + t.getBySource());
        }
        if (t.getRecordedOnChain() != null) {
            println("  链上登记 : " + t.getRecordedOnChain().getCount() + " 笔（"
                    + String.format("%.2f%%", t.getRecordedOnChain().getRate()) + "）");
        }
        if (t.getGas() != null) {
            println("  Gas      : 总 " + t.getGas().getTotal() + " / 均 " + t.getGas().getAvg());
        }
        if (t.getLatency() != null) {
            println("  耗时     : " + t.getLatency().getCount() + " 笔，平均 " + t.getLatency().getAvg() + " ms");
        }

        List<Task> tasks = taskMapper.selectList(null);
        Map<TaskStatus, Long> taskByStatus = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        println("【任务统计】");
        if (tasks.isEmpty()) {
            println("  暂无任务记录");
        } else {
            taskByStatus.forEach((k, v) -> println("  " + k + " : " + v));
        }

        List<ChainReceipt> receipts = chainReceiptMapper.selectList(null);
        if (!receipts.isEmpty()) {
            receipts.sort(Comparator.comparing(ChainReceipt::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
            println("【最近 " + Math.min(limit, receipts.size()) + " 条回执】");
            receipts.stream().limit(limit).forEach(r -> println(
                    "  " + shortId(r.getRequestId()) + " | " + r.getStatus()
                            + " | epoch=" + r.getBlockNumber() + " | " + time(r.getTimestamp())));
        }
    }

    private Coordinator findCoordinator() {
        return coordinatorService.getOne(new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
    }

    private void printMap(Map<?, ?> map, int indent) {
        String pad = "  ".repeat(indent);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> nested) {
                println(pad + e.getKey() + ":");
                printMap(nested, indent + 1);
            } else if (v instanceof List<?> list) {
                println(pad + e.getKey() + ":");
                list.forEach(item -> println(pad + "  - " + item));
            } else {
                println(pad + e.getKey() + " : " + v);
            }
        }
    }

    private static int parseLimit(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法 limit: " + raw);
        }
    }

    private static String time(Long ts) {
        return ts == null ? "-" : TIME_FMT.format(new Date(ts));
    }

    private static String shortId(String id) {
        return id == null ? "-" : id.length() > 8 ? id.substring(0, 8) : id;
    }

    private static void divider(String title) {
        println("======== " + title + " ========");
    }

    private static void println(Object o) {
        System.out.println(o);
    }
}