package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.example.entity.Coordinator;
import org.example.enums.CoordinatorState;
import org.example.enums.TaskStatus;
import org.example.util.ServiceDiscoveryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ExperimentFinishService {

    private static final int STABLE_ROUNDS = 3;

    private final AtomicLong lastRequestCount = new AtomicLong(-1);

    private final AtomicInteger stableRounds = new AtomicInteger(0);

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ChainRequestService chainRequestService;

    @Autowired
    private ChainReceiptService chainReceiptService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NodeCommandService nodeCommandService;

    @Autowired
    private ExperimentReportService experimentReportService;

    @Autowired
    private ServiceDiscoveryUtil serviceDiscoveryUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Scheduled(fixedDelay = 10_000)
    public void detectNaturalFinish() {
        Coordinator coordinator = findCoordinator();
        if (coordinator == null) {
            return;
        }
        if (coordinator.getGlobalState() != CoordinatorState.RUNNING) {
             
            return;
        }
        if (isNaturallyFinished()) {
            log.info("[SYSTEM-CONTROLLER] 检测到实验自然结束条件满足，自动结束实验并生成报告");
            finish("自然结束（交易全部灌入并处理完成）");
        }
    }

    public String finish(String reason) {
        Coordinator coordinator = findCoordinator();
        if (coordinator == null) {
            log.warn("[SYSTEM-CONTROLLER] 协调者尚未初始化，无法结束实验（请先执行 init）");
            return null;
        }
        if (coordinator.getGlobalState() == CoordinatorState.FINISHED) {
            log.info("[SYSTEM-CONTROLLER] 实验已处于 FINISHED 状态，跳过重复结束（如需重新开始请执行 start）");
            return null;
        }
         
        nodeCommandService.stopAll();
         
        coordinatorService.updateGlobalState(CoordinatorState.FINISHED);
        log.info("[SYSTEM-CONTROLLER] 实验已结束，协调者状态置为 FINISHED（reason={}）", reason);
         
        String reportPath = experimentReportService.generateReport(reason);
        resetDetection();
        return reportPath;
    }

    public void resetDetection() {
        lastRequestCount.set(-1);
        stableRounds.set(0);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private boolean isNaturallyFinished() {
        long requestCount = chainRequestService.count();
        if (requestCount <= 0) {
            return false;
        }
         
        long receiptCount = chainReceiptService.count();
        if (receiptCount < requestCount) {
            log.debug("[SYSTEM-CONTROLLER] 自然结束检测：回执数 {} < 请求数 {}，仍有交易未处理完成",
                    receiptCount, requestCount);
            return false;
        }
         
        long pending = taskService.count(new LambdaQueryWrapper<org.example.entity.Task>()
                .in(org.example.entity.Task::getStatus, TaskStatus.PENDING, TaskStatus.PROCESSING));
        if (pending > 0) {
            log.debug("[SYSTEM-CONTROLLER] 自然结束检测：仍有 {} 个任务未完成", pending);
            return false;
        }
         
        if (!allAgentsFinished()) {
            return false;
        }
         
        long last = lastRequestCount.get();
        if (last == requestCount) {
            int rounds = stableRounds.incrementAndGet();
            if (rounds >= STABLE_ROUNDS) {
                return true;
            }
            log.debug("[SYSTEM-CONTROLLER] 自然结束检测：请求总数已稳定 {} 轮（当前 {}），还需 {} 轮",
                    rounds, requestCount, STABLE_ROUNDS - rounds);
            return false;
        }
         
        lastRequestCount.set(requestCount);
        stableRounds.set(0);
        return false;
    }

    private boolean allAgentsFinished() {
        List<ServiceInstance> agents = serviceDiscoveryUtil.getServiceInstancesByPrefix("agent-node");
        if (agents.isEmpty()) {
            log.debug("[SYSTEM-CONTROLLER] 自然结束检测：未发现 agent 节点，跳过账单灌完条件");
            return true;
        }
        for (ServiceInstance agent : agents) {
            try {
                String url = String.format("http://%s/agent/status", agent.getServiceId());
                Result<?> result = restTemplate.getForObject(url, Result.class);
                if (result == null || result.getData() == null) {
                    log.debug("[SYSTEM-CONTROLLER] agent {} 状态查询返回空，跳过该条件", agent.getServiceId());
                    continue;
                }
                Map<?, ?> data = (Map<?, ?>) result.getData();
                Object finishedObj = data.get("finished");
                Object lastLineNoObj = data.get("lastLineNo");
                boolean finished = Boolean.TRUE.equals(finishedObj);
                long lastLineNo = lastLineNoObj instanceof Number n ? n.longValue() : 0L;
                if (!finished && lastLineNo > 0) {
                    log.debug("[SYSTEM-CONTROLLER] agent {} 正在灌单（进度行号 {}，finished=false），实验继续",
                            agent.getServiceId(), lastLineNo);
                    return false;
                }
                if (!finished) {
                    log.debug("[SYSTEM-CONTROLLER] agent {} 从未灌入数据（未启用自动灌单），跳过该条件", agent.getServiceId());
                }
            } catch (Exception e) {
                log.debug("[SYSTEM-CONTROLLER] agent {} 状态查询失败（跳过该条件）：{}", agent.getServiceId(), e.getMessage());
            }
        }
        return true;
    }

    private Coordinator findCoordinator() {
        return coordinatorService.getOne(new LambdaQueryWrapper<Coordinator>().last("LIMIT 1"));
    }
}