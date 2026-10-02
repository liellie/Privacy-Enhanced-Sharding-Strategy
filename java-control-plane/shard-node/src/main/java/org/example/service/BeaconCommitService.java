package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.config.ShardLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;

@Slf4j
@Service
public class BeaconCommitService {

    @Value("${shard.beacon.enabled:true}")
    private boolean enabled;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ShardLifecycle shardLifecycle;

    private final SecureRandom random = new SecureRandom();

    private volatile BigInteger pendingEpoch;

    private volatile byte[] pendingSecret;

    private volatile boolean revealed;

    @Scheduled(fixedDelayString = "${shard.beacon.interval-ms:${shard.metrics.interval-ms:5000}}")
    public void tick() {
        if (!enabled) {
            return;
        }
         
        if (!shardLifecycle.isReady()) {
            return;
        }
        try {
            BigInteger current = blockchainService.currentEpoch();
            if (pendingEpoch == null) {
                commitNext(current);
            } else {
                revealOrAdvance(current);
            }
        } catch (Exception e) {
             
            log.warn("信标轮询失败（尽力而为，下个周期重试）：{}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private void commitNext(BigInteger current) {
        BigInteger target = current.add(BigInteger.ONE);
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        try {
            String tx = blockchainService.beaconCommitSecret(target, secret);
            pendingEpoch = target;
            pendingSecret = secret;
            revealed = false;
            log.info("信标承诺已提交：epoch={}, tx={}", target, tx);
        } catch (Exception e) {
             
            log.warn("信标承诺提交失败（epoch={}，下个周期重试）：{}", target, e.getMessage());
        }
    }

    private void revealOrAdvance(BigInteger current) {
         
        if (current.compareTo(pendingEpoch) >= 0) {
            log.debug("信标目标 epoch={} 已过期（当前={}），清空进入下一轮", pendingEpoch, current);
            pendingEpoch = null;
            pendingSecret = null;
            revealed = false;
            return;
        }
        if (revealed) {
             
            return;
        }
        try {
            String tx = blockchainService.beaconReveal(pendingEpoch, pendingSecret);
            revealed = true;
            log.info("信标秘密已揭示：epoch={}, tx={}", pendingEpoch, tx);
        } catch (Exception e) {
             
            log.warn("信标揭示失败（epoch={}，下个周期重试）：{}", pendingEpoch, e.getMessage());
        }
    }
}