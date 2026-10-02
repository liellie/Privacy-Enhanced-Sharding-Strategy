package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Agent;

import java.util.List;

public interface AgentService extends IService<Agent> {

    Agent registerAgent(Agent agent);

    boolean online(String agentId);

    boolean offline(String agentId);

    Integer getAgentShardId(String agentId);

    List<Agent> listByOrganization(String organizationId);
}
