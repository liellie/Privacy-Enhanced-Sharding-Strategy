package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Shard;

import java.util.List;

public interface ShardService extends IService<Shard> {

    Shard registerShard(Shard shard);

    boolean updateLoadMetrics(Shard shard);

    Shard selectOptimalShard();

    Shard getBusiestShard();

    Shard getMostIdleShard();

    List<Shard> listAll();
}