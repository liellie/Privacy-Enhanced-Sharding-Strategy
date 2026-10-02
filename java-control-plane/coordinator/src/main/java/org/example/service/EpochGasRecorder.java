package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
@Slf4j
@Service
public class EpochGasRecorder {

    @Value("${coordinator.experiment.output-dir:outputs/onchain}")
    private String outputDir;

    @Value("${coordinator.experiment.run-id:}")
    private String configuredRunId;

    @Value("${coordinator.experiment.committee-size:4}")
    private int committeeSize;

    @Value("${coordinator.experiment.num-liars:1}")
    private int numLiars;

    @Value("${coordinator.experiment.lie-mode:lower}")
    private String lieMode;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Map<String, Object>> epochs = new ArrayList<>();

    public void record(int epoch, List<EpochGasStep> steps) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("epoch", epoch);
        List<Map<String, Object>> stepMaps = new ArrayList<>();
        for (EpochGasStep s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", s.name());
            m.put("gas_used", s.gasUsed() == null ? null : s.gasUsed().longValue());
            m.put("wall_ms", s.wallMs());
            stepMaps.add(m);
        }
        entry.put("steps", stepMaps);
        synchronized (epochs) {
            epochs.add(entry);
        }
    }

    public void writeJson() {
        try {
            File dir = new File(outputDir);
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("无法创建输出目录：{}", outputDir);
                return;
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("run_id", resolveRunId("epoch-gas"));
            root.put("epochs", new ArrayList<>(epochs));
            File out = new File(dir, "onchain_epoch_gas.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, root);
            log.info("已写 {}：{} 轮 Epoch 开销", out.getAbsolutePath(), epochs.size());
        } catch (Exception e) {
            log.warn("写 onchain_epoch_gas.json 失败：{}", e.getMessage());
        }
        mergeByzantineFragments();
    }

    public void mergeByzantineFragments() {
        try {
            File dir = new File(outputDir);
            if (!dir.exists()) {
                return;
            }
            File[] frags = dir.listFiles((d, name) ->
                    name.startsWith("onchain_byzantine.") && name.endsWith(".json"));
            if (frags == null || frags.length == 0) {
                return;
            }
            List<Map<String, Object>> shards = new ArrayList<>();
            for (File f : frags) {
                Map<String, Object> m = objectMapper.readValue(f, Map.class);
                shards.add(m);
            }
            shards.sort((a, b) -> Integer.compare(
                    ((Number) a.getOrDefault("shard", -1)).intValue(),
                    ((Number) b.getOrDefault("shard", -1)).intValue()));
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("run_id", resolveRunId("byzantine"));
             
            root.put("committee_size", committeeSize);
            root.put("num_liars", numLiars);
            root.put("lie_mode", lieMode);
            root.put("shards", shards);
            File out = new File(dir, "onchain_byzantine.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, root);
            for (File f : frags) {
                if (!f.delete()) {
                    log.warn("删除片段失败：{}", f.getName());
                }
            }
            log.info("已合并 {}：{} 个分片片段", out.getAbsolutePath(), shards.size());
        } catch (Exception e) {
            log.warn("合并 onchain_byzantine.json 失败：{}", e.getMessage());
        }
    }

    private String resolveRunId(String prefix) {
        return (configuredRunId == null || configuredRunId.isBlank())
                ? prefix + "-" + System.currentTimeMillis()
                : configuredRunId;
    }
}
