package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.config.CoordinatorLifecycle;
import org.example.entity.Task;
import org.example.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TaskCoordinatorService {

    @Value("${coordinator.scheduler.enabled}")
    private boolean enabled;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CoordinatorLifecycle coordinatorLifecycle;

    @Scheduled(fixedDelayString = "${coordinator.epoch-duration}")
    public void scheduledProcess() {
        if (!enabled) {
            return;
        }
         
        if (!coordinatorLifecycle.isReady()) {
            return;
        }
        processPending();
    }

    public void processPending() {
        List<Task> pending = taskService.listPendingTasks();
        if (pending.isEmpty()) {
            return;
        }
        log.info("开始协调任务：待处理 {} 个", pending.size());
        for (Task task : pending) {
            processOne(task);
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private void processOne(Task task) {
        String taskId = task.getTaskId();
        try {
            taskService.updateStatus(taskId, TaskStatus.PROCESSING);
             
            boolean done = taskService.updateStatus(taskId, TaskStatus.COMPLETED);
            if (done) {
                log.info("任务协调完成：taskId={}, type={}, sourceShard={}, targetShard={}",
                        taskId, task.getTaskType(), task.getSourceShardId(), task.getTargetShardId());
            }
        } catch (Exception e) {
            log.error("任务协调失败：taskId={}，置 FAILED：{}", taskId, e.getMessage(), e);
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        }
    }
}