package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Shard;
import org.example.mapper.ShardMapper;
import org.example.service.ShardService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ShardServiceImpl extends ServiceImpl<ShardMapper, Shard> implements ShardService {

    private static final double W_CPU = 0.4;
     
    private static final double W_MEM = 0.3;
     
    private static final double W_DISK = 0.2;
     
    private static final double W_NET = 0.1;

    @Override
    public Shard registerShard(Shard shard) {
        save(shard);
        return shard;
    }

    @Override
    public boolean updateLoadMetrics(Shard shard) {
        return lambdaUpdate()
                .eq(Shard::getShardId, shard.getShardId())
                .set(Shard::getCpuUsage, shard.getCpuUsage())
                .set(Shard::getMemoryUsage, shard.getMemoryUsage())
                .set(Shard::getDiskIoLoad, shard.getDiskIoLoad())
                .set(Shard::getNetworkBandwidth, shard.getNetworkBandwidth())
                .set(Shard::getCurrentTps, shard.getCurrentTps())
                .update();
    }

    @Override
    public Shard selectOptimalShard() {
         
        return list().stream()
                .min(Comparator.comparingDouble(this::calculateLoad))
                .orElse(null);
    }

    @Override
    public Shard getBusiestShard() {
        return getOne(new LambdaQueryWrapper<Shard>()
                .orderByDesc(Shard::getCurrentTps)
                .last("LIMIT 1"));
    }

    @Override
    public Shard getMostIdleShard() {
        return getOne(new LambdaQueryWrapper<Shard>()
                .orderByAsc(Shard::getCurrentTps)
                .last("LIMIT 1"));
    }

    @Override
    public List<Shard> listAll() {
        return list();
    }

    private double calculateLoad(Shard shard) {
        return W_CPU * safe(shard.getCpuUsage())
                + W_MEM * safe(shard.getMemoryUsage())
                + W_DISK * safe(shard.getDiskIoLoad())
                + W_NET * safe(shard.getNetworkBandwidth());
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}