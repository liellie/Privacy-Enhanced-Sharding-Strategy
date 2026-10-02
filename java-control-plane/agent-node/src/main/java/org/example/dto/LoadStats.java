package org.example.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadStats {

    private int total;

    private int submitted;

    private int skipped;

    private int failed;

    private int crossShard;

    private boolean recordedOnChain;

    private long lastLineNo;

    private boolean finished;
}