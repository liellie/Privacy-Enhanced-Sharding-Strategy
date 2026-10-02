package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {

    private Double cpuUsage;

    private Double memoryUsage;

    private Double diskIoLoad;

    private Double networkBandwidth;

    private long collectedAt;
}