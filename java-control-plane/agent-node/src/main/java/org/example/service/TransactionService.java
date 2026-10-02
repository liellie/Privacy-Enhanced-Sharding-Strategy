package org.example.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.BlockchainService;
import org.example.dto.LoadStats;
import org.example.dto.OpOutcome;
import org.example.dto.RoutingResult;
import org.example.entity.AgentNode;
import org.example.entity.ChainReceipt;
import org.example.entity.ChainRequest;
import org.example.entity.ChainTranscription;
import org.example.enums.ReceiptStatus;
import org.example.util.TransactionFileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class TransactionService {

    private static final long BASE_GAS = 21_000L;

    @Autowired
    private ChainRequestService chainRequestService;

    @Autowired
    private ChainReceiptService chainReceiptService;

    @Autowired
    private CoordinatorService coordinatorService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ChainTranscriptionService chainTranscriptionService;

    @Autowired
    private AgentNode agentNode;

    @Value("${agent.transaction-file}")
    private String transactionFile = "data/transactions.txt";

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    public ChainReceipt submit(ChainRequest request) {
         
        if (request == null || request.getSenderAddress() == null || request.getSenderAddress().isBlank()) {
            throw new IllegalArgumentException("senderAddress 不能为空");
        }

        boolean alreadyPersisted = false;
        String requestId = request.getRequestId();
        if (requestId != null && !requestId.isEmpty()) {
            ChainReceipt receipt = chainReceiptService.getByRequestId(requestId);
            if (receipt != null) {
                log.info("requestId={} 已有回执，直接复用（幂等）", requestId);
                return receipt;
            }
            ChainRequest existing = chainRequestService.getByRequestId(requestId);
            if (existing != null) {
                 
                log.warn("requestId={} 请求已存在但回执缺失，补全处理", requestId);
                request = existing;
                alreadyPersisted = true;
            }
        }

        ChainRequest saved = alreadyPersisted ? request : chainRequestService.submit(request);
        long submitTime = saved.getTimestamp() == null ? System.currentTimeMillis() : saved.getTimestamp();

        RouteOutcome outcome = processRouted(saved);

        String onChainTxHash = null;

        ChainReceipt receipt = outcome.getReceipt();
        recordTranscription(saved, receipt, outcome, "AGENT", agentNode.getAgentId(), submitTime, onChainTxHash);

        return receipt;
    }

    public ChainReceipt getReceipt(String requestId) {
        return chainReceiptService.getByRequestId(requestId);
    }

    public OpOutcome submitForExperiment(ChainRequest request, long seq) {
        if (request == null || request.getSenderAddress() == null || request.getSenderAddress().isBlank()) {
            throw new IllegalArgumentException("senderAddress 不能为空");
        }
        long submitMs = System.currentTimeMillis();

        boolean alreadyPersisted = false;
        String requestId = request.getRequestId();
        if (requestId != null && !requestId.isEmpty()) {
            ChainReceipt receipt = chainReceiptService.getByRequestId(requestId);
            if (receipt != null) {
                return OpOutcome.builder()
                        .seq(seq).receipt(receipt).crossShard(false)
                        .submitMs(submitMs).confirmMs(System.currentTimeMillis())
                        .gasUsed(receipt.getGasUsed() == null ? null : BigInteger.valueOf(receipt.getGasUsed()))
                        .build();
            }
            ChainRequest existing = chainRequestService.getByRequestId(requestId);
            if (existing != null) {
                request = existing;
                alreadyPersisted = true;
            }
        }

        if (request.getTimestamp() == null) {
            request.setTimestamp(submitMs);
        }
        ChainRequest saved = alreadyPersisted ? request : chainRequestService.submit(request);

        RouteOutcome outcome = processRouted(saved);
        ChainReceipt receipt = outcome.getReceipt();
        long savedSubmit = saved.getTimestamp() == null ? submitMs : saved.getTimestamp();

        // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
        OnChainWrite write = null;
        String opError = null;
        try {
            write = executeOnChainOperation(saved, outcome);
        } catch (Exception e) {
            log.warn("实验操作真实链上写入失败（记 aborted）：requestId={}, {}", saved.getRequestId(), e.getMessage());
            opError = e.getMessage();
        }

        recordTranscription(saved, receipt, outcome, "AGENT", agentNode.getAgentId(),
                savedSubmit, write == null ? null : write.txHash());

        return OpOutcome.builder()
                .seq(seq)
                .receipt(receipt)
                .crossShard(outcome.isCrossShard())
                .senderShardId(outcome.getSenderShardId())
                .receiverShardId(outcome.getReceiverShardId())
                .submitMs(savedSubmit)
                .routeTime(outcome.getRouteTime())
                .crossCheckTime(outcome.getCrossCheckTime())
                .confirmMs(write == null ? System.currentTimeMillis() : write.confirmMs())
                .gasUsed(write == null ? null : write.gasUsed())
                .txHash(write == null ? null : write.txHash())
                .blockNumber(write == null ? null : write.blockNumber())
                .error(opError)
                .build();
    }

    private OnChainWrite executeOnChainOperation(ChainRequest saved, RouteOutcome outcome) {
        byte[] txId = sha256Bytes("exp-cross-tx:" + saved.getRequestId());
        byte[] metadataRoot = sha256Bytes("exp-asset-meta:" + saved.getRequestId());
        byte[] encryptedRef = sha256Bytes("exp-asset-ref:" + saved.getRequestId());

        if (!outcome.isCrossShard()) {
             
            BigInteger shardId = BigInteger.valueOf(
                    outcome.getSenderShardId() == null ? 0 : outcome.getSenderShardId());
            BlockchainService.TxInfo info =
                    blockchainService.createAssetInfo(metadataRoot, encryptedRef, shardId);
            return new OnChainWrite(info.txHash(), info.gasUsed(),
                    info.blockNumber().longValue(), System.currentTimeMillis());
        }

        BigInteger epoch = blockchainService.currentEpoch();
        ensureCommittee(epoch);

        BigInteger senderShard = BigInteger.valueOf(outcome.getSenderShardId());
        BigInteger receiverShard = BigInteger.valueOf(outcome.getReceiverShardId());
        java.util.List<BigInteger> inputShards = new java.util.ArrayList<>();
        inputShards.add(senderShard);
        if (!senderShard.equals(receiverShard)) {
            inputShards.add(receiverShard);
        }

        BigInteger gasSum = BigInteger.ZERO;
        BlockchainService.TxInfo init = blockchainService.initiateCrossShard(txId, epoch, inputShards);
        gasSum = gasSum.add(init.gasUsed());

        List<byte[]> objects = List.of(metadataRoot);
        for (BigInteger shard : inputShards) {
            gasSum = gasSum.add(
                    blockchainService.voteCrossShardQuorum(txId, epoch, shard, true, objects));
        }

        BlockchainService.TxInfo commit = blockchainService.commitCrossShard(txId);
        gasSum = gasSum.add(commit.gasUsed());

        return new OnChainWrite(commit.txHash(), gasSum,
                commit.blockNumber().longValue(), System.currentTimeMillis());
    }

    private void ensureCommittee(BigInteger epoch) {
        if (blockchainService.committeeFormed(epoch)) {
            return;
        }
        try {
            blockchainService.formCommitteeInfo(epoch);
            log.info("已为 epoch {} 形成委员会", epoch);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "epoch " + epoch + " 委员会未形成且无法形成（beacon 可能未定稿）: "
                            + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    public LoadStats loadFromFile(int offset, int limit) {
        if (offset < 0) {
            offset = 0;
        }
        long remaining = (limit <= 0) ? Long.MAX_VALUE : limit;
        LoadStats stats = LoadStats.builder().build();
        String path = transactionFile;

        String line;
        long lineNo = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null && stats.getSubmitted() < remaining) {
                lineNo++;
                if (lineNo <= offset) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                ChainRequest parsed = TransactionFileParser.parseLine(line);
                if (parsed == null) {
                    log.warn("第 {} 行无法解析，已跳过：{}", lineNo, abbreviate(line));
                    stats.setFailed(stats.getFailed() + 1);
                    continue;
                }
                stats.setTotal(stats.getTotal() + 1);
                try {
                     
                    if (chainReceiptService.getByRequestId(parsed.getRequestId()) != null) {
                        stats.setSkipped(stats.getSkipped() + 1);
                        continue;
                    }
                     
                    ChainRequest existingReq = chainRequestService.getByRequestId(parsed.getRequestId());
                    ChainRequest saved = (existingReq != null) ? existingReq : chainRequestService.submit(parsed);
                    long submitTime = saved.getTimestamp() == null ? System.currentTimeMillis() : saved.getTimestamp();
                    RouteOutcome outcome = processRouted(saved);
                    if (outcome.isCrossShard()) {
                        stats.setCrossShard(stats.getCrossShard() + 1);
                    }
                    stats.setSubmitted(stats.getSubmitted() + 1);
                     
                    recordTranscription(saved, outcome.getReceipt(), outcome, "LOAD",
                            agentNode.getAgentId(), submitTime, null);
                } catch (Exception e) {
                    log.error("第 {} 行交易处理失败：{}", lineNo, e.getMessage(), e);
                    stats.setFailed(stats.getFailed() + 1);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取交易数据文件失败: " + path, e);
        }

        stats.setLastLineNo(lineNo);
        stats.setFinished(line == null);

        stats.setRecordedOnChain(false);
        log.info("交易批量加载完成：total={}, submitted={}, skipped={}, failed={}, crossShard={}, lastLineNo={}, finished={}, recordedOnChain={}",
                stats.getTotal(), stats.getSubmitted(), stats.getSkipped(),
                stats.getFailed(), stats.getCrossShard(), stats.getLastLineNo(),
                stats.isFinished(), stats.isRecordedOnChain());
        return stats;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private RouteOutcome processRouted(ChainRequest saved) {

        RoutingResult rr = chainRequestService.route(saved);
        long routeTime = System.currentTimeMillis();
        saved.setTargetShardId(rr.getShardId());
        chainRequestService.updateById(saved);

        CrossShardCheck check = checkCrossShardOnChain(saved);
        long crossCheckTime = System.currentTimeMillis();

        ChainReceipt receipt = buildReceipt(saved);
        long receiptTime = System.currentTimeMillis();

        return RouteOutcome.builder()
                .receipt(receipt)
                .crossShard(check.isCrossShard())
                .senderShardId(check.getSenderShardId())
                .receiverShardId(check.getReceiverShardId())
                .routeSource(rr.getSource())
                .routeTime(routeTime)
                .crossCheckTime(crossCheckTime)
                .receiptTime(receiptTime)
                .build();
    }

    private CrossShardCheck checkCrossShardOnChain(ChainRequest request) {
        if (request.getReceiverAddress() == null || request.getReceiverAddress().isBlank()) {
            log.debug("缺少 receiverAddress，无法链上跨片判定，按同片处理");
            return CrossShardCheck.of(false, null, null);
        }
        try {
            Integer senderShard = blockchainService.getOptimalShard(request.getSenderAddress()).intValue();
            Integer receiverShard = blockchainService.getOptimalShard(request.getReceiverAddress()).intValue();
            boolean cross = !senderShard.equals(receiverShard);
            log.debug("链上跨片判定：sender={}→shard{}, receiver={}→shard{}, cross={}",
                    request.getSenderAddress(), senderShard,
                    request.getReceiverAddress(), receiverShard, cross);
            return CrossShardCheck.of(cross, senderShard, receiverShard);
        } catch (Exception e) {
             
            log.warn("链上跨片判定失败，按同片处理：{}", e.getMessage());
            return CrossShardCheck.of(false, null, null);
        }
    }

    private ChainReceipt buildReceipt(ChainRequest saved) {
        ChainReceipt receipt = ChainReceipt.builder()
                .requestId(saved.getRequestId())
                .txHash(resolveTxHash(saved))
                .status(ReceiptStatus.SUCCESS)
                .blockNumber(currentEpoch())
                .gasUsed(BASE_GAS)
                .logs(List.of("executed-by-agent-" + saved.getTargetShardId()))
                .build();
        chainReceiptService.createReceipt(receipt);
        return receipt;
    }

    private Long currentEpoch() {
        Long epoch = coordinatorService.getCurrentEpoch();
        return epoch == null ? 0L : epoch;
    }

    private void recordTranscription(ChainRequest saved, ChainReceipt receipt, RouteOutcome outcome,
                                     String source, String processor, long submitTime, String onChainTxHash) {
        try {
            ChainTranscription transcription = ChainTranscription.builder()
                    .requestId(saved.getRequestId())
                    .txHash(receipt.getTxHash())
                    .txType(saved.getTxType())
                    .senderAddress(saved.getSenderAddress())
                    .receiverAddress(saved.getReceiverAddress())
                    .assetId(saved.getAssetId())
                    .amount(saved.getAmount())
                    .senderShardId(outcome.getSenderShardId())
                    .receiverShardId(outcome.getReceiverShardId())
                    .targetShardId(saved.getTargetShardId())
                    .crossShard(outcome.isCrossShard())
                    .routeSource(outcome.getRouteSource())
                    .source(source)
                    .processor(processor)
                    .submitTime(submitTime)
                    .routeTime(outcome.getRouteTime())
                    .crossCheckTime(outcome.getCrossCheckTime())
                    .receiptTime(outcome.getReceiptTime())
                    .totalLatency(outcome.getReceiptTime() - submitTime)
                    .recordedOnChain(onChainTxHash != null)
                    .onChainTxHash(onChainTxHash)
                    .blockNumber(receipt.getBlockNumber())
                    .gasUsed(receipt.getGasUsed())
                    .status(receipt.getStatus())
                    .errorMessage(receipt.getErrorMessage())
                    .logs(receipt.getLogs() == null ? null : String.valueOf(receipt.getLogs()))
                    .build();
            chainTranscriptionService.createTranscription(transcription);
        } catch (Exception e) {
             
            log.warn("记录链上转录失败（不影��主链路）：requestId={}, 错���={}",
                    saved.getRequestId(), e.getMessage());
        }
    }

    private String resolveTxHash(ChainRequest request) {
        String payload = request.getPayload();
         
        if (payload != null && payload.startsWith("0x") && payload.length() == 66) {
            return payload;
        }
         
        return "0x" + sha256Hex(request.getRequestId() + ":" + request.getTargetShardId());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static byte[] sha256Bytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String abbreviate(String line) {
        return line.length() > 120 ? line.substring(0, 120) + "..." : line;
    }

    @Data
    @Builder
    private static class RouteOutcome {
         
        private ChainReceipt receipt;
         
        private boolean crossShard;
         
        private Integer senderShardId;
         
        private Integer receiverShardId;
         
        private String routeSource;
         
        private long routeTime;
         
        private long crossCheckTime;
         
        private long receiptTime;
    }

    @Data
    private static class CrossShardCheck {
        private final boolean crossShard;
        private final Integer senderShardId;
        private final Integer receiverShardId;

        static CrossShardCheck of(boolean crossShard, Integer senderShardId, Integer receiverShardId) {
            return new CrossShardCheck(crossShard, senderShardId, receiverShardId);
        }
    }

    private record OnChainWrite(String txHash, BigInteger gasUsed, long blockNumber, long confirmMs) {
    }
}