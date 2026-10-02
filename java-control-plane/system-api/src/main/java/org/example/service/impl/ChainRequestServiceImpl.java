package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.dto.RoutingResult;
import org.example.entity.ChainRequest;
import org.example.entity.Agent;
import org.example.mapper.ChainRequestMapper;
import org.example.service.AgentService;
import org.example.service.ChainRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChainRequestServiceImpl extends ServiceImpl<ChainRequestMapper, ChainRequest>
        implements ChainRequestService {

    @Autowired
    private AgentService agentService;

    @Autowired
    private BlockchainService blockchainService;

    @Override
    public ChainRequest submit(ChainRequest request) {
         
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        }
         
        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }
        save(request);
        return request;
    }

    @Override
    public boolean isCrossShard(ChainRequest request) {
        Integer senderShardId = resolveShardByAddress(request.getSenderAddress());
        Integer targetShardId = request.getTargetShardId();
         
        if (senderShardId == null || targetShardId == null) {
            return true;
        }
         
        return !senderShardId.equals(targetShardId);
    }

    @Override
    public Integer routeToShard(ChainRequest request) {
        return route(request).getShardId();
    }

    @Override
    public RoutingResult route(ChainRequest request) {
        try {
            Integer shard = blockchainService.routeToShard(
                    request.getSenderAddress(), request.getReceiverAddress());
            if (shard != null) {
                log.debug("链上路由：request={}, sender={} -> shard {}",
                        request.getRequestId(), request.getSenderAddress(), shard);
                return RoutingResult.builder().shardId(shard).source("ONCHAIN").build();
            }
        } catch (Exception e) {
            log.warn("链上路由失败，降级为 targetShardId：{}", e.getMessage());
        }
        return RoutingResult.builder()
                .shardId(request.getTargetShardId())
                .source("FALLBACK")
                .build();
    }

    @Override
    public ChainRequest getByRequestId(String requestId) {
        return getById(requestId);
    }

    @Override
    public List<ChainRequest> listTimeoutRequests(long timeoutMillis) {
        long deadline = System.currentTimeMillis() - timeoutMillis;
        return list(new LambdaQueryWrapper<ChainRequest>()
                .lt(ChainRequest::getTimestamp, deadline));
    }

    private Integer resolveShardByAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
         
        Agent agent = agentService.getOne(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getPublicKey, address)
                .last("LIMIT 1"));
         
        if (agent == null) {
            agent = agentService.getById(address);
        }
        return agent == null ? null : agent.getCurrentShardId();
    }
}