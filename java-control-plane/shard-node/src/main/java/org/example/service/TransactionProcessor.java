package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainRequest;
import org.example.entity.ChainTranscription;
import org.example.enums.ReceiptStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class TransactionProcessor {

    private static final long BASE_GAS = 21_000L;

    @Autowired
    private ChainRequestService chainRequestService;

    @Autowired
    private ChainReceiptService chainReceiptService;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private ShardSelfService shardSelfService;

    @Autowired
    private TpsTracker tpsTracker;

    @Autowired
    private ChainTranscriptionService chainTranscriptionService;

    public ChainReceipt process(ChainRequest request) {
         
        if (request == null || request.getRequestId() == null || request.getRequestId().isBlank()) {
            throw new IllegalArgumentException("requestId 不能为空");
        }
        String requestId = request.getRequestId();

        ChainReceipt existing = chainReceiptService.getByRequestId(requestId);
        if (existing != null) {
            log.info("requestId={} 已有回执，直接复用（幂等）", requestId);
            return existing;
        }

        Integer shardId = shardSelfService.shardId();
        if (shardId == null) {
            throw new IllegalStateException(
                    "本分片尚未初始化（shard 表无本节点 ip+port 记录），请先调用 POST /shard/init");
        }

        ChainRequest saved = chainRequestService.getByRequestId(requestId);
        if (saved == null) {
            if (request.getTargetShardId() == null) {
                request.setTargetShardId(shardId);
            }
            saved = chainRequestService.submit(request);
        }

        ChainReceipt receipt = ChainReceipt.builder()
                .requestId(requestId)
                .txHash(resolveTxHash(requestId, saved.getTargetShardId()))
                .status(ReceiptStatus.SUCCESS)
                .blockNumber(blockNumber())
                .gasUsed(BASE_GAS)
                .logs(List.of("executed-by-shard-" + saved.getTargetShardId()))
                .build();
        try {
            chainReceiptService.createReceipt(receipt);
        } catch (DuplicateKeyException e) {
             
            ChainReceipt concurrent = chainReceiptService.getByRequestId(requestId);
            if (concurrent != null) {
                log.info("requestId={} 并发写入冲突，复用已有回执（幂等）", requestId);
                return concurrent;
            }
            throw e;
        }

        tpsTracker.increment();

        try {
            Long submitTime = saved.getTimestamp();
            long receiptTime = System.currentTimeMillis();
            ChainTranscription transcription = ChainTranscription.builder()
                    .requestId(requestId)
                    .txHash(receipt.getTxHash())
                    .txType(saved.getTxType())
                    .senderAddress(saved.getSenderAddress())
                    .receiverAddress(saved.getReceiverAddress())
                    .assetId(saved.getAssetId())
                    .amount(saved.getAmount())
                    .senderShardId(saved.getTargetShardId())
                    .targetShardId(saved.getTargetShardId())
                    .crossShard(false)
                    .routeSource("NONE")
                    .source("SHARD")
                    .processor("shard-" + shardId)
                    .submitTime(submitTime)
                    .receiptTime(receiptTime)
                    .totalLatency(submitTime == null ? null : receiptTime - submitTime)
                    .recordedOnChain(false)
                    .blockNumber(receipt.getBlockNumber())
                    .gasUsed(receipt.getGasUsed())
                    .status(receipt.getStatus())
                    .logs(String.valueOf(receipt.getLogs()))
                    .build();
            chainTranscriptionService.createTranscription(transcription);
        } catch (Exception e) {
            log.warn("记录链上转录失败（不影响主链路）：requestId={}, 错误={}", requestId, e.getMessage());
        }

        log.info("交易处理完成：requestId={}, shardId={}, txHash={}",
                requestId, saved.getTargetShardId(), receipt.getTxHash());
        return receipt;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private Long blockNumber() {
        Long epoch = coordinatorService.getCurrentEpoch();
        return epoch == null ? 0L : epoch;
    }

    private static String resolveTxHash(String requestId, Integer targetShardId) {
        return "0x" + sha256Hex(requestId + ":" + targetShardId);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}