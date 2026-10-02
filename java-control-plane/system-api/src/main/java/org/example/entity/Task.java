package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import org.example.enums.TaskStatus;
import org.example.enums.TaskType;

@Data
@Builder
@TableName("task")
public class Task {
     
    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String requestId;

    private TaskType taskType;

    private Integer sourceShardId;

    private Integer targetShardId;

    private TaskStatus status;

    private Long createTime;
}