package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Task;
import org.example.enums.TaskStatus;

import java.util.List;

public interface TaskService extends IService<Task> {

    Task createCrossShardTask(String requestId, int sourceShardId, int targetShardId);

    Task createStateSyncTask(String requestId, int sourceShardId, int targetShardId);

    Task createRebalanceTask(String requestId, int sourceShardId, int targetShardId);

    boolean updateStatus(String taskId, TaskStatus status);

    Task getByRequestId(String requestId);

    List<Task> listPendingTasks();
}