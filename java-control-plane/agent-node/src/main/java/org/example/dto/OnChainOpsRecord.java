package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnChainOpsRecord {

    private long seq;

    private String type;

    /** committed / aborted */
    private String status;

    private long submitMs;

    private long confirmMs;

    private long gasUsed;

    private String txHash;

    private Long blockNumber;

    private int srcShard;

    private int dstShard;
}
