package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.util.ServiceDiscoveryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
@Slf4j
@Service
public class NodeCommandService {

    @Autowired
    private ServiceDiscoveryUtil serviceDiscoveryUtil;

    @Autowired
    private RestTemplate restTemplate;

    public void initAll() {
        initCoordinator();
        initShard();
        initAgent();
        log.info("[SYSTEM-CONTROLLER] 所有节点初始化流程完成");
    }

    private void initCoordinator() {
        postToAll("coordinator", "/coordinator/init");
    }

    private void initShard() {
        postToAll("shard-node", "/shard/init");
    }

    private void initAgent() {
        postToAll("agent-node", "/agent/init");
    }

    public void startAll() {
        startCoordinator();
        startShard();
        log.info("[SYSTEM-CONTROLLER] 所有节点开始运行指令下发完成");
    }

    public void stopAll() {
        stopCoordinator();
        stopShard();
        log.info("[SYSTEM-CONTROLLER] 所有节点暂停指令下发完成");
    }

    private void startCoordinator() {
        postToAll("coordinator", "/coordinator/start");
    }

    private void startShard() {
        postToAll("shard-node", "/shard/start");
    }

    private void stopCoordinator() {
        postToAll("coordinator", "/coordinator/stop");
    }

    private void stopShard() {
        postToAll("shard-node", "/shard/stop");
    }

    public void shutdownAll() {
        shutdownCoordinator();
        shutdownAgent();
        shutdownShard();
        log.info("[SYSTEM-CONTROLLER] 所有节点关闭流程完成");
    }

    private void shutdownCoordinator() {
        postToAll("coordinator", "/coordinator/shutdown");
    }

    private void shutdownAgent() {
        postToAll("agent-node", "/agent/shutdown");
    }

    private void shutdownShard() {
        postToAll("shard-node", "/shard/shutdown");
    }

    private void postToAll(String servicePrefix, String path) {
        List<ServiceInstance> instances = serviceDiscoveryUtil.getServiceInstancesByPrefix(servicePrefix);
        if (instances.isEmpty()) {
            log.warn("[SYSTEM-CONTROLLER] 未找到任何 {} 实例", servicePrefix);
            return;
        }
        log.info("[SYSTEM-CONTROLLER] 找到 {} 个 {} 节点，开始下发命令 {}", instances.size(), servicePrefix, path);
        for (ServiceInstance instance : instances) {
            String nodeName = instance.getServiceId();
            try {
                String url = String.format("http://%s%s", nodeName, path);
                restTemplate.postForObject(url, buildEmptyEntity(), String.class);
                log.info("[SYSTEM-CONTROLLER] 命令执行成功 | 节点: {} | 命令: {}", nodeName, path);
            } catch (Exception e) {
                log.error("[SYSTEM-CONTROLLER] 命令执行失败 | 节点: {} | 命令: {} | 错误: {}", nodeName, path, e.getMessage());
            }
        }
    }

    private HttpEntity<Void> buildEmptyEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(headers);
    }
}