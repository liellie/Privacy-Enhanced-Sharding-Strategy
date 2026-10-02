package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.OnChainOpsRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OnChainOpsReportService {

    private OnChainOpsReportService() {
    }

    public static Map<String, Object> summary(List<OnChainOpsRecord> ops) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overall", bucket(ops, null));
        summary.put("intra", bucket(ops, "intra"));
        summary.put("cross", bucket(ops, "cross"));
        return summary;
    }

    private static Map<String, Object> bucket(List<OnChainOpsRecord> ops, String typeFilter) {
        List<OnChainOpsRecord> sub = new ArrayList<>();
        for (OnChainOpsRecord r : ops) {
            if (typeFilter == null || typeFilter.equals(r.getType())) {
                sub.add(r);
            }
        }
        long committed = sub.stream().filter(r -> "committed".equals(r.getStatus())).count();
        long aborted = sub.size() - committed;
        List<Long> latencies = new ArrayList<>();
        long gasSum = 0;
        for (OnChainOpsRecord r : sub) {
            gasSum += r.getGasUsed();
            if ("committed".equals(r.getStatus()) && r.getConfirmMs() >= r.getSubmitMs()) {
                latencies.add(r.getConfirmMs() - r.getSubmitMs());
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", sub.size());
        m.put("committed", committed);
        m.put("aborted", aborted);
        m.put("commit_rate_pct", sub.isEmpty() ? 0.0
                : Math.round(committed * 10000.0 / sub.size()) / 100.0);
        m.put("p50_ms", percentile(latencies, 50));
        m.put("p95_ms", percentile(latencies, 95));
        m.put("p99_ms", percentile(latencies, 99));
        m.put("avg_gas", sub.isEmpty() ? 0.0 : Math.round(gasSum * 10.0 / sub.size()) / 10.0);
        return m;
    }

    public static Double percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int n = sorted.size();
        int idx = (int) Math.ceil(p / 100.0 * n) - 1;
        idx = Math.max(0, Math.min(n - 1, idx));
        return (double) sorted.get(idx);
    }

    public static Map<String, Object> summary(CopyOnWriteArrayList<OnChainOpsRecord> ops) {
        return summary(new ArrayList<>(ops));
    }
}
