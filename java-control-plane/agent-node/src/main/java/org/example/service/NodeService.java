package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Agent;
import org.example.entity.Coordinator;
import org.example.entity.Shard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NodeService {

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardService shardService;

    @Autowired
    private AgentService agentService;

    public Agent initialize(Agent agent) {
        String agentId = agent.getAgentId();
        String organizationId = agent.getOrganizationId();
        String publicKey = agent.getPublicKey();
         
        if (agentId == null || agentId.isBlank()
                || organizationId == null || organizationId.isBlank()
                || publicKey == null || publicKey.isBlank()) {
            throw new IllegalArgumentException(String.format(
                    "参数未配置（agentId='%s', organizationId='%s', publicKey='%s'），"
                            + "请检查配置文件 agent.* 或请求参数", agentId, organizationId, publicKey));
        }
        log.info("Agent 节点初始化开始：agentId={}, organization={}", agentId, organizationId);

        Coordinator coordinator = coordinatorService.start();
        log.info("协调者就绪：epoch={}, state={}", coordinator.getCurrentEpoch(), coordinator.getGlobalState());

        Shard shard = shardService.selectOptimalShard();
        if (shard == null) {
            throw new IllegalStateException(
                    "当前没有任何可用分片（shard 表为空），请先启动并注册分片节点后再初始化 Agent 节点。");
        }

        Agent existing = agentService.getById(agentId);
        Agent result;
        if (existing == null) {
            result = agentService.registerAgent(Agent.builder()
                    .agentId(agentId)
                    .organizationId(organizationId)
                    .publicKey(publicKey)
                    .currentShardId(shard.getShardId())
                    .build());
        } else {
            existing.setCurrentShardId(shard.getShardId());
            existing.setOrganizationId(organizationId);
            existing.setPublicKey(publicKey);
            agentService.updateById(existing);
            result = existing;
        }

        log.info("Agent 节点初始化完成：agentId={}, 归属分片 shardId={}",
                result.getAgentId(), result.getCurrentShardId());
        return result;
    }
}