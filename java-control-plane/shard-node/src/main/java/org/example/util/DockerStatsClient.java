package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.SystemMetrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

@Slf4j
public class DockerStatsClient {

    private static final double NET_REF_BYTES_PER_SEC = 125_000_000.0;

    private static final double DISK_REF_BYTES_PER_SEC = 50_000_000.0;

    private final ObjectMapper objectMapper;
    private final String containerName;
    private final String dockerHost;

    private long prevBlkioBytes;
    private long prevNetBytes;
    private long prevSampleTime;

    private boolean initialized;

    public DockerStatsClient(String dockerHost, String containerName) {
        this.dockerHost = (dockerHost == null || dockerHost.isBlank())
                ? "unix:///var/run/docker.sock" : dockerHost;
         
        this.containerName = containerName == null ? "" : containerName.replaceFirst("^/+", "");
        this.objectMapper = new ObjectMapper();
    }

    public SystemMetrics collect() {
        JsonNode stats = fetchStats();
        if (stats == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        try {
            double cpu = computeCpu(stats);                        
            double mem = computeMemory(stats);                     
            long blkioBytes = readBlkioBytes(stats);               
            long netBytes = readNetBytes(stats);                   
            double disk = rateRatio(blkioBytes, prevBlkioBytes, now, DISK_REF_BYTES_PER_SEC);
            double net = rateRatio(netBytes, prevNetBytes, now, NET_REF_BYTES_PER_SEC);
             
            prevBlkioBytes = blkioBytes;
            prevNetBytes = netBytes;
            prevSampleTime = now;
            initialized = true;
            return SystemMetrics.builder()
                    .cpuUsage(cpu)
                    .memoryUsage(mem)
                    .diskIoLoad(disk)
                    .networkBandwidth(net)
                    .collectedAt(now)
                    .build();
        } catch (Exception e) {
            log.warn("Docker stats 解析失败（container={}）：{}", containerName, e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private JsonNode fetchStats() {
        if (containerName.isEmpty()) {
            return null;
        }
        String path = "/v1.41/containers/" + containerName + "/stats?stream=false";
        try {
            String body = httpGet(path);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("Docker stats 获取失败（container={}, host={}）：{}",
                    containerName, dockerHost, e.getMessage());
            return null;
        }
    }

    private String httpGet(String path) throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        byte[] response;
        if (dockerHost.startsWith("unix://")) {
            response = exchangeOverChannel(
                    SocketChannel.open(UnixDomainSocketAddress.of(dockerHost.substring("unix://".length()))),
                    request);
        } else {
            String hostPort = dockerHost
                    .replaceFirst("^tcp://", "")
                    .replaceFirst("^http://", "")
                    .replaceFirst("^https://", "");
            String[] hp = hostPort.split(":");
            int port = hp.length > 1 ? Integer.parseInt(hp[1]) : 2375;
            response = exchangeOverChannel(
                    SocketChannel.open(new InetSocketAddress(hp[0], port)),
                    request);
        }
        String raw = new String(response, StandardCharsets.UTF_8);
        int headerEnd = raw.indexOf("\r\n\r\n");
        if (headerEnd < 0) {
            throw new IOException("非法 HTTP 响应（无响应头分隔）");
        }
        String statusLine = raw.substring(0, raw.indexOf("\r\n"));
        if (!statusLine.contains(" 200 ")) {
            throw new IOException("Docker API 返回非 200：" + statusLine);
        }
        return raw.substring(headerEnd + 4);
    }

    private static byte[] exchangeOverChannel(SocketChannel channel, String request) throws IOException {
        try (SocketChannel ch = channel) {
            ByteBuffer writeBuf = ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8));
            while (writeBuf.hasRemaining()) {
                ch.write(writeBuf);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteBuffer readBuf = ByteBuffer.allocate(16 * 1024);
            int n;
            while ((n = ch.read(readBuf)) > 0) {
                readBuf.flip();
                out.write(readBuf.array(), readBuf.position(), n);
                readBuf.clear();
            }
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private static double computeCpu(JsonNode stats) {
        JsonNode cpu = stats.path("cpu_stats");
        JsonNode pre = stats.path("precpu_stats");
        long total = cpu.path("cpu_usage").path("total_usage").asLong(0);
        long system = cpu.path("system_cpu_usage").asLong(0);
        int onlineCpus = cpu.path("online_cpus").asInt(0);
        long preTotal = pre.path("cpu_usage").path("total_usage").asLong(0);
        long preSystem = pre.path("system_cpu_usage").asLong(0);
        long cpuDelta = total - preTotal;
        long systemDelta = system - preSystem;
        if (systemDelta <= 0 || onlineCpus <= 0) {
            return 0.0;
        }
        return clamp((double) cpuDelta / systemDelta * onlineCpus);
    }

    private static double computeMemory(JsonNode stats) {
        JsonNode mem = stats.path("memory_stats");
        long usage = mem.path("usage").asLong(0);
        long limit = mem.path("limit").asLong(0);
        if (limit <= 0) {
            return 0.0;
        }
        return clamp((double) usage / limit);
    }

    private static long readBlkioBytes(JsonNode stats) {
        long bytes = 0;
        JsonNode io = stats.path("blkio_stats").path("io_service_bytes_recursive");
        if (io.isArray()) {
            for (JsonNode item : io) {
                String op = item.path("op").asText("");
                if ("read".equals(op) || "write".equals(op)) {
                    bytes += item.path("value").asLong(0);
                }
            }
        }
        return bytes;
    }

    private static long readNetBytes(JsonNode stats) {
        long bytes = 0;
        JsonNode networks = stats.path("networks");
        if (networks.isObject()) {
            var fields = networks.fields();
            while (fields.hasNext()) {
                JsonNode iface = fields.next().getValue();
                bytes += iface.path("rx_bytes").asLong(0) + iface.path("tx_bytes").asLong(0);
            }
        }
        return bytes;
    }

    private double rateRatio(long current, long previous, long now, double refBytesPerSec) {
        if (!initialized || previous <= 0) {
            return 0.0;
        }
        long delta = current - previous;
        long deltaMs = now - prevSampleTime;
        if (delta <= 0 || deltaMs <= 0) {
            return 0.0;
        }
        double bps = delta * 1000.0 / deltaMs;
        return clamp(bps / refBytesPerSec);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
    }
}