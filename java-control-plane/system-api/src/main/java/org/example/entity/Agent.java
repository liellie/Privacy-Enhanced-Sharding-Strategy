package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.enums.AgentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent")
public class Agent {
     
    @TableId(value = "agent_id", type = IdType.INPUT)
    private String agentId;

    private String organizationId;

    private Integer currentShardId;

    private String publicKey;

    private AgentStatus status;
}