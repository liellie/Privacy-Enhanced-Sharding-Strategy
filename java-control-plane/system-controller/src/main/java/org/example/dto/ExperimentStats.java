package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentStats {

    private ReceiptStats receipt;

    private TranscriptionStats transcription;

    private int shardCount;

    private int agentCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptStats {
         
        private long total;
         
        private long success;
         
        private long failed;
         
        private double successRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrossShardStats {
         
        private long count;
         
        private double rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnChainStats {
         
        private long count;
         
        private double rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GasStats {
         
        private long total;
         
        private double avg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatencyStats {
         
        private long count;
         
        private double avg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionStats {
         
        private long total;
         
        private CrossShardStats crossShard;
         
        private Map<Integer, Long> byShard;
         
        private Map<Long, Long> byEpoch;
         
        private Map<String, Long> byTxType;
         
        private Map<String, Long> bySource;
         
        private OnChainStats recordedOnChain;
         
        private GasStats gas;
         
        private LatencyStats latency;
    }
}
