package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.SystemMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class SystemMetricsCollector {

    private static final double REFERENCE_BYTES_PER_SEC = 125_000_000.0;

    @Value("${shard.docker.enabled:true}")
    private boolean dockerEnabled;

    @Value("${shard.docker.container-name:}")
    private String dockerContainerName;

    @Value("${shard.docker.docker-host:unix:///var/run/docker.sock}")
    private String dockerHost;

    private final com.sun.management.OperatingSystemMXBean osBean;

    private volatile DockerStatsClient dockerClient;

    private final AtomicLong dockerFailCount = new AtomicLong();

    private Method bytesSentMethod;
    private Method bytesReceivedMethod;

    private long prevNetBytes;
    private long prevSampleTime;

    public SystemMetricsCollector() {
        this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        cacheNetMethods();
        this.prevNetBytes = readNetBytes();
        this.prevSampleTime = System.currentTimeMillis();
    }

    public SystemMetrics collect() {
        SystemMetrics dockerMetrics = tryCollectFromDocker();
        if (dockerMetrics != null) {
            return dockerMetrics;
        }
         
        return SystemMetrics.builder()
                .cpuUsage(clamp(osBean.getCpuLoad()))
                .memoryUsage(clamp(1.0 - safeRatio(osBean.getFreeMemorySize(), osBean.getTotalMemorySize())))
                .diskIoLoad(diskUsage())
                .networkBandwidth(networkUsage())
                .collectedAt(System.currentTimeMillis())
                .build();
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private SystemMetrics tryCollectFromDocker() {
        DockerStatsClient client = dockerClient();
        if (client == null) {
            return null;
        }
        SystemMetrics metrics = client.collect();
        if (metrics != null) {
             
            dockerFailCount.set(0);
            return metrics;
        }
        long fails = dockerFailCount.incrementAndGet();
         
        if (fails == 1 || fails % 60 == 0) {
            log.warn("Docker 容器资源采集不可用（累计 {} 次失败，container={}），回退宿主机采集",
                    fails, dockerContainerName);
        }
        return null;
    }

    private DockerStatsClient dockerClient() {
        if (dockerClient == null && dockerEnabled
                && dockerContainerName != null && !dockerContainerName.isBlank()) {
            synchronized (this) {
                if (dockerClient == null) {
                    dockerClient = new DockerStatsClient(dockerHost, dockerContainerName);
                    log.info("已启用 Docker 容器资源采集：container={}, dockerHost={}",
                            dockerContainerName, dockerHost);
                }
            }
        }
        return dockerClient;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private double diskUsage() {
        try {
            FileStore store = Files.getFileStore(Paths.get(System.getProperty("user.dir")));
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            if (total <= 0) {
                return 0.0;
            }
            return clamp(1.0 - (double) usable / total);
        } catch (IOException e) {
            log.warn("磁盘负载采集失败：{}", e.getMessage());
            return 0.0;
        }
    }

    private double networkUsage() {
        long now = System.currentTimeMillis();
        long bytes = readNetBytes();
        long deltaBytes = bytes - prevNetBytes;
        long deltaMs = now - prevSampleTime;
        prevNetBytes = bytes;
        prevSampleTime = now;
        if (deltaMs <= 0 || deltaBytes <= 0) {
            return 0.0;
        }
        double bps = deltaBytes * 1000.0 / deltaMs;
        return clamp(bps / REFERENCE_BYTES_PER_SEC);
    }

    private void cacheNetMethods() {
        try {
            this.bytesSentMethod = osBean.getClass().getMethod("getBytesSent");
            this.bytesReceivedMethod = osBean.getClass().getMethod("getBytesReceived");
        } catch (NoSuchMethodException e) {
            log.debug("当前平台不提供网络字节统计（getBytesSent/getBytesReceived），网络指标按 0 处理");
        }
    }

    private long readNetBytes() {
        if (bytesSentMethod == null || bytesReceivedMethod == null) {
            return 0;
        }
        try {
            long sent = (Long) bytesSentMethod.invoke(osBean);
            long received = (Long) bytesReceivedMethod.invoke(osBean);
             
            return (sent < 0 || received < 0) ? 0 : sent + received;
        } catch (Exception e) {
            log.debug("网络字节统计读取失败：{}", e.getMessage());
            return 0;
        }
    }

    private static double safeRatio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
    }
}