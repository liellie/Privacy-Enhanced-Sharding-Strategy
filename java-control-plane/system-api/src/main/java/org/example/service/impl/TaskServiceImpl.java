package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Task;
import org.example.enums.TaskStatus;
import org.example.enums.TaskType;
import org.example.mapper.TaskMapper;
import org.example.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Override
    public Task createCrossShardTask(String requestId, int sourceShardId, int targetShardId) {
        return createTask(TaskType.CROSS_SHARD_TX, requestId, sourceShardId, targetShardId);
    }

    @Override
    public Task createStateSyncTask(String requestId, int sourceShardId, int targetShardId) {
        return createTask(TaskType.STATE_SYNC, requestId, sourceShardId, targetShardId);
    }

    @Override
    public Task createRebalanceTask(String requestId, int sourceShardId, int targetShardId) {
        return createTask(TaskType.REBALANCE, requestId, sourceShardId, targetShardId);
    }

    @Override
    public boolean updateStatus(String taskId, TaskStatus status) {
        return lambdaUpdate()
                .eq(Task::getTaskId, taskId)
                .set(Task::getStatus, status)
                .update();
    }

    @Override
    public Task getByRequestId(String requestId) {
        return getOne(new LambdaQueryWrapper<Task>()
                .eq(Task::getRequestId, requestId)
                .last("LIMIT 1"));
    }

    @Override
    public List<Task> listPendingTasks() {
        return list(new LambdaQueryWrapper<Task>()
                .in(Task::getStatus, TaskStatus.PENDING, TaskStatus.PROCESSING)
                .orderByAsc(Task::getCreateTime));
    }

    private Task createTask(TaskType taskType, String requestId, int sourceShardId, int targetShardId) {
        Task task = Task.builder()
                .taskId(UUID.randomUUID().toString().replace("-", ""))
                .requestId(requestId)
                .taskType(taskType)
                .sourceShardId(sourceShardId)
                .targetShardId(targetShardId)
                .status(TaskStatus.PENDING)
                .createTime(System.currentTimeMillis())
                .build();
        save(task);
        return task;
    }
}