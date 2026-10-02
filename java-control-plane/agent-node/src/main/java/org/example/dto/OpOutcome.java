package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpOutcome {

    private long seq;

    private org.example.entity.ChainReceipt receipt;

    private boolean crossShard;

    private Integer senderShardId;

    private Integer receiverShardId;

    private long submitMs;

    private long routeTime;

    private long crossCheckTime;

    private long confirmMs;

    private BigInteger gasUsed;

    private String txHash;

    private Long blockNumber;

    private String error;
}
