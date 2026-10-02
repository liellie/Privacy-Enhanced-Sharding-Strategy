package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.config.ShardLifecycle;
import org.example.entity.Shard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShardNodeService {

    @Autowired
    private ShardService shardService;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardLifecycle shardLifecycle;

    public Shard init(Shard shard) {
        String ipAddress = shard.getIpAddress();
        Integer port = shard.getPort();
         
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException(String.format(
                    "参数未配置（ipAddress='%s'），请检查配置文件 shard.* 或请求参数", ipAddress));
        }
        if (port == null || port <= 0) {
            throw new IllegalArgumentException("参数未配置（port=" + port + "），请检查配置文件 shard.* 或请求参数");
        }
        log.info("=== 分片节点初始化开始，ip={}, port={} ===", ipAddress, port);

        Shard existing = shardService.getOne(new LambdaQueryWrapper<Shard>()
                .eq(Shard::getIpAddress, ipAddress)
                .eq(Shard::getPort, port)
                .last("LIMIT 1"));
        Shard result;
        if (existing == null) {
            result = shardService.registerShard(Shard.builder()
                    .ipAddress(ipAddress)
                    .port(port)
                    .build());
            log.info("分片已注册：shardId={}, ip={}, port={}", result.getShardId(), ipAddress, port);
        } else {
            result = existing;
            log.info("分片已存在，复用记录：shardId={}, ip={}, port={}", existing.getShardId(), ipAddress, port);
        }

        int totalShards = shardService.listAll().size();
        coordinatorService.updateShardCount(totalShards);
        log.info("已同步协调者分片总数：totalShards={}", totalShards);

        log.info("=== 分片节点初始化完成：shardId={}, ip={}, port={} ===",
                result.getShardId(), ipAddress, port);
         
        shardLifecycle.markInitialized();
        return result;
    }

    public void start() {
        if (!shardLifecycle.isInitialized()) {
            throw new IllegalStateException("分片节点尚未初始化，请先执行 init 命令（或调用 POST /shard/init）");
        }
        shardLifecycle.markStarted();
        log.info("=== 分片节点已开始运行：负载上报 / 信标承诺等定时任务已激活 ===");
    }

    public void stop() {
        shardLifecycle.markStopped();
        log.info("=== 分片节点已暂停：定时任务已关闭（可执行 start 恢复） ===");
    }
}