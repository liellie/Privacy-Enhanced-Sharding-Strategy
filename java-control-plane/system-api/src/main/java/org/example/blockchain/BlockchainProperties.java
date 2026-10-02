package org.example.blockchain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainProperties {

    private String rpcUrl = "http://localhost:8545";

    private Long chainId = 20260801L;

    private String privateKey = "";

    private List<String> committeePrivateKeys = new ArrayList<>();

    private Long gasLimit = 8_000_000L;

    private Contracts contracts = new Contracts();

    @Data
    public static class Contracts {
        private String epochBeacon;
        private String resourceOracle;
        private String affinityManager;
        private String crossShardCounter;
        private String registry;
        private String controller;
        private String validatorRegistry;
        private String coordinator;
        private String assetRegistry;
    }
}