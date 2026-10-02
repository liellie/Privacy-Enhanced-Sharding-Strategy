package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Shard;
import org.example.entity.ShardNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShardSelfService {

    @Autowired
    private ShardService shardService;

    @Autowired
    private ShardNode shardNode;

    private volatile Integer shardId;

    public Integer shardId() {
        if (shardId != null) {
            return shardId;
        }
        Shard shard = shardService.getOne(new LambdaQueryWrapper<Shard>()
                .eq(Shard::getIpAddress, shardNode.getIpAddress())
                .eq(Shard::getPort, shardNode.getPort())
                .last("LIMIT 1"));
        if (shard == null) {
            return null;
        }
        shardId = shard.getShardId();
        log.info("已解析本节点 shardId={}（ip={}, port={}）",
                shardId, shardNode.getIpAddress(), shardNode.getPort());
        return shardId;
    }

    public void invalidate() {
        this.shardId = null;
    }
}