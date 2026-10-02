package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shard")
public class Shard {
     
    @TableId(value = "shard_id", type = IdType.AUTO)
    private Integer shardId;

    private String ipAddress;

    private Integer port;

    private Double cpuUsage;

    private Double memoryUsage;

    private Double diskIoLoad;

    private Double networkBandwidth;

    private Long currentTps;
}