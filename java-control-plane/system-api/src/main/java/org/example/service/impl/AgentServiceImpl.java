package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Agent;
import org.example.enums.AgentStatus;
import org.example.mapper.AgentMapper;
import org.example.service.AgentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Override
    public Agent registerAgent(Agent agent) {
         
        agent.setStatus(AgentStatus.ONLINE);
         
        save(agent);
        return agent;
    }

    @Override
    public boolean online(String agentId) {
         
        return lambdaUpdate()
                .eq(Agent::getAgentId, agentId)
                .set(Agent::getStatus, AgentStatus.ONLINE)
                .update();
    }

    @Override
    public boolean offline(String agentId) {
        return lambdaUpdate()
                .eq(Agent::getAgentId, agentId)
                .set(Agent::getStatus, AgentStatus.OFFLINE)
                .update();
    }

    @Override
    public Integer getAgentShardId(String agentId) {
         
        Agent agent = getById(agentId);
        return agent == null ? null : agent.getCurrentShardId();
    }

    @Override
    public List<Agent> listByOrganization(String organizationId) {
         
        return list(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getOrganizationId, organizationId));
    }
}
