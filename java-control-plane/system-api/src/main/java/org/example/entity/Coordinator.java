package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.enums.CoordinatorState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coordinator")
public class Coordinator {
     
    @TableId(value = "coordinator_id", type = IdType.INPUT)
    private String coordinatorId;

    private Long currentEpoch;

    private Long epochStartTime;

    private Long epochDuration;

    private CoordinatorState globalState;

    private Integer totalShards;
}