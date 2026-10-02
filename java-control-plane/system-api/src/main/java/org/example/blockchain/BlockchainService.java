package org.example.blockchain;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.blockchain.contract.AffinityManager;
import org.example.blockchain.contract.CrossShardCoordinator;
import org.example.blockchain.contract.CrossShardCounter;
import org.example.blockchain.contract.DigitalAssetRegistry;
import org.example.blockchain.contract.EnhancedShardRegistryV2;
import org.example.blockchain.contract.EpochBeacon;
import org.example.blockchain.contract.MultiObjectiveController;
import org.example.blockchain.contract.ResourceOracle;
import org.example.blockchain.contract.ValidatorRegistry;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class BlockchainService {

    private final BlockchainProperties props;

    private final AtomicReference<Web3j> web3jRef = new AtomicReference<>();

    private final AtomicReference<EpochBeacon> epochBeaconRef = new AtomicReference<>();
    private final AtomicReference<ResourceOracle> oracleRef = new AtomicReference<>();
    private final AtomicReference<AffinityManager> affinityRef = new AtomicReference<>();
    private final AtomicReference<CrossShardCounter> counterRef = new AtomicReference<>();
    private final AtomicReference<EnhancedShardRegistryV2> registryRef = new AtomicReference<>();
    private final AtomicReference<MultiObjectiveController> controllerRef = new AtomicReference<>();
    private final AtomicReference<ValidatorRegistry> validatorRef = new AtomicReference<>();
    private final AtomicReference<CrossShardCoordinator> coordinatorRef = new AtomicReference<>();
    private final AtomicReference<DigitalAssetRegistry> assetRef = new AtomicReference<>();

    public BlockchainService(BlockchainProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        log.info("BlockchainService 初始化：rpc={}, chainId={}, registry={}",
                props.getRpcUrl(), props.getChainId(), props.getContracts().getRegistry());
    }

    @PreDestroy
    public void shutdown() {
        Web3j w = web3jRef.get();
        if (w != null) {
            w.shutdown();
            log.info("BlockchainService 已关闭连接");
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private Web3j web3j() {
        return web3jRef.updateAndGet(existing -> existing != null ? existing
                : Web3j.build(new HttpService(props.getRpcUrl())));
    }

    private Credentials credentials() {
        String key = props.getPrivateKey();
        if (key == null || key.isBlank()) {
             
            return Credentials.create(ECKeyPair.create(BigInteger.ZERO));
        }
        return Credentials.create(key.startsWith("0x") ? key : "0x" + key);
    }

    private void requirePrivateKey() {
        String key = props.getPrivateKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "区块链私钥未配置（blockchain.private-key），无法发送交易。"
                            + "请在配置中填入 chain/.env 的 DEPLOYER_PRIVATE_KEY。");
        }
    }

    private ContractGasProvider gasProvider() {
        return new StaticGasProvider(BigInteger.ZERO, BigInteger.valueOf(props.getGasLimit()));
    }

    private <T extends Contract> T lazy(AtomicReference<T> ref,
                                        java.util.function.Supplier<T> supplier) {
        return ref.updateAndGet(existing -> existing != null ? existing : supplier.get());
    }

    private EpochBeacon epochBeacon() {
        return lazy(epochBeaconRef, () -> EpochBeacon.load(
                props.getContracts().getEpochBeacon(), web3j(), credentials(), gasProvider()));
    }

    private ResourceOracle oracle() {
        return lazy(oracleRef, () -> ResourceOracle.load(
                props.getContracts().getResourceOracle(), web3j(), credentials(), gasProvider()));
    }

    private AffinityManager affinity() {
        return lazy(affinityRef, () -> AffinityManager.load(
                props.getContracts().getAffinityManager(), web3j(), credentials(), gasProvider()));
    }

    private CrossShardCounter counter() {
        return lazy(counterRef, () -> CrossShardCounter.load(
                props.getContracts().getCrossShardCounter(), web3j(), credentials(), gasProvider()));
    }

    private EnhancedShardRegistryV2 registry() {
        return lazy(registryRef, () -> EnhancedShardRegistryV2.load(
                props.getContracts().getRegistry(), web3j(), credentials(), gasProvider()));
    }

    private MultiObjectiveController controller() {
        return lazy(controllerRef, () -> MultiObjectiveController.load(
                props.getContracts().getController(), web3j(), credentials(), gasProvider()));
    }

    private ValidatorRegistry validator() {
        return lazy(validatorRef, () -> ValidatorRegistry.load(
                props.getContracts().getValidatorRegistry(), web3j(), credentials(), gasProvider()));
    }

    private CrossShardCoordinator coordinator() {
        return lazy(coordinatorRef, () -> CrossShardCoordinator.load(
                props.getContracts().getCoordinator(), web3j(), credentials(), gasProvider()));
    }

    private DigitalAssetRegistry assetRegistry() {
        return lazy(assetRef, () -> DigitalAssetRegistry.load(
                props.getContracts().getAssetRegistry(), web3j(), credentials(), gasProvider()));
    }

    public String accountAddress() {
        return credentials().getAddress();
    }

    public record TxInfo(String txHash, BigInteger gasUsed, BigInteger blockNumber) {
        @Override
        public String toString() {
            return "TxInfo{tx=" + txHash + ", gas=" + gasUsed + ", block=" + blockNumber + "}";
        }
    }

    public String beaconCommitSecret(BigInteger epoch, byte[] secret) {
        if (secret == null || secret.length != 32) {
            throw new IllegalArgumentException("secret 必须为 32 字节");
        }
        byte[] addr = Numeric.hexStringToByteArray(accountAddress());
        byte[] packed = new byte[52];
        System.arraycopy(secret, 0, packed, 0, 32);
        System.arraycopy(addr, 0, packed, 32, 20);
        return beaconCommit(epoch, Hash.sha3(packed));
    }

    public String recordCrossShard(BigInteger crossCount, BigInteger total) {
        return send(counter().recordBatch(crossCount, total));
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    private <T> T call(RemoteFunctionCall<T> call) {
        try {
            return call.send();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("链上调用失败: " + e.getMessage(), e);
        }
    }

    private String send(RemoteFunctionCall<TransactionReceipt> call) {
        return sendInfo(call).txHash();
    }

    public TxInfo sendInfo(RemoteFunctionCall<TransactionReceipt> call) {
        requirePrivateKey();
        try {
            TransactionReceipt receipt = call.send();
            String txHash = receipt.getTransactionHash();
            BigInteger gasUsed = receipt.getGasUsed();
            BigInteger blockNumber = receipt.getBlockNumber();
            log.info("交易已确认：tx={}, gasUsed={}, block={}", txHash, gasUsed, blockNumber);
            return new TxInfo(txHash, gasUsed, blockNumber);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("交易发送失败: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    public BigInteger currentEpoch() {
        return call(epochBeacon().currentEpoch());
    }

    public byte[] beacon(BigInteger epoch) {
        return call(epochBeacon().beacon(epoch));
    }

    public BigInteger getOptimalShard(String orgAddress) {
        return call(registry().getOptimalShard(orgAddress));
    }

    public BigInteger loadImbalance() {
        return call(registry().loadImbalance());
    }

    public BigInteger privacyRisk() {
        return call(registry().privacyRisk());
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    public String submitReading(BigInteger epoch, BigInteger shardId,
                                BigInteger cpu, BigInteger mem, BigInteger stor, BigInteger net,
                                BigInteger prediction) {
        return send(oracle().submit(epoch, shardId, cpu, mem, stor, net, prediction));
    }

    public String beaconCommit(BigInteger epoch, byte[] commitment) {
        return send(epochBeacon().commit(epoch, commitment));
    }

    public String beaconReveal(BigInteger epoch, byte[] secret) {
        return send(epochBeacon().reveal(epoch, secret));
    }

    public String beaconFinalize(BigInteger epoch) {
        return send(epochBeacon().finalize(epoch));
    }

    public TxInfo beaconFinalizeInfo(BigInteger epoch) {
        return sendInfo(epochBeacon().finalize(epoch));
    }

    public TxInfo finalizeOracleShardInfo(BigInteger epoch, BigInteger shardId) {
        return sendInfo(oracle().finalizeShard(epoch, shardId));
    }

    public TxInfo syncShardInfo(BigInteger epoch, BigInteger shardId) {
        return sendInfo(registry().syncShard(epoch, shardId));
    }

    public TxInfo adjustResourceWeightsInfo() {
        return sendInfo(registry().adjustResourceWeights());
    }

    public TxInfo publishPrivatizedLoadInfo(BigInteger epoch, BigInteger shardId) {
        return sendInfo(registry().publishPrivatizedLoad(epoch, shardId));
    }

    public TxInfo controllerStepInfo(BigInteger epoch) {
        return sendInfo(controller().step(epoch));
    }

    public TxInfo recordCrossShardBatchInfo(BigInteger crossCount, BigInteger total) {
        return sendInfo(counter().recordBatch(crossCount, total));
    }

    // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.

    public TxInfo initiateCrossShard(byte[] txId, BigInteger epoch, List<BigInteger> inputShards) {
        return sendInfo(coordinator().initiate(txId, epoch, inputShards));
    }

    public TxInfo voteCrossShard(byte[] txId, BigInteger shardId, boolean approve, List<byte[]> objects) {
        return sendInfo(coordinator().vote(txId, shardId, approve, objects));
    }

    public TxInfo voteCrossShardAs(String privateKey, byte[] txId, BigInteger shardId,
                                   boolean approve, List<byte[]> objects) {
        requirePrivateKey();
        try {
            Credentials c = Credentials.create(normalizeKey(privateKey));
            CrossShardCoordinator coordinator = CrossShardCoordinator.load(
                    props.getContracts().getCoordinator(), web3j(), c, gasProvider());
            TransactionReceipt receipt = coordinator.vote(txId, shardId, approve, objects).send();
            return new TxInfo(receipt.getTransactionHash(), receipt.getGasUsed(),
                    receipt.getBlockNumber());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("投票交易发送失败: " + e.getMessage(), e);
        }
    }

    public TxInfo commitCrossShard(byte[] txId) {
        return sendInfo(coordinator().commit(txId));
    }

    public TxInfo abortCrossShard(byte[] txId) {
        return sendInfo(coordinator().abort(txId));
    }

    public BigInteger quorumOf(BigInteger epoch, BigInteger shardId) {
        return call(coordinator().quorumOf(epoch, shardId));
    }

    public BigInteger committeeSize(BigInteger epoch, BigInteger shardId) {
        return call(validator().committeeSize(epoch, shardId));
    }

    public boolean committeeFormed(BigInteger epoch) {
        return Boolean.TRUE.equals(call(validator().committeeFormed(epoch)));
    }

    public boolean isCommitteeMember(BigInteger epoch, BigInteger shardId, String who) {
        return Boolean.TRUE.equals(call(validator().isCommitteeMember(epoch, shardId, who)));
    }

    @SuppressWarnings("unchecked")
    public List<String> committeeOf(BigInteger epoch, BigInteger shardId) {
        return (List<String>) call(validator().committeeOf(epoch, shardId));
    }

    public BigInteger voteCrossShardQuorum(byte[] txId, BigInteger epoch, BigInteger shardId,
                                           boolean approve, List<byte[]> objects) {
        List<String> members = committeeOf(epoch, shardId);
        if (members == null || members.isEmpty()) {
            throw new IllegalStateException(
                    "epoch " + epoch + " shard " + shardId + " 委员会为空，无法投票");
        }
        BigInteger sum = BigInteger.ZERO;
        int voted = 0;
        for (String member : members) {
            String key = committeeKeyFor(member);
            if (key == null) {
                throw new IllegalStateException(
                        "未配置 shard " + shardId + " 委员会成员 " + member + " 的私钥"
                                + "（blockchain.committee-private-keys，与 deployed.json"
                                + " accounts.shardNodes 对齐）");
            }
            TxInfo info = voteCrossShardAs(key, txId, shardId, approve, objects);
            sum = sum.add(info.gasUsed());
            voted += 1;
        }
        BigInteger quorum = quorumOf(epoch, shardId);
        if (voted < quorum.intValue()) {
            throw new IllegalStateException(
                    "shard " + shardId + " 实际投票 " + voted + " 票 < 法定 " + quorum + " 票");
        }
        return sum;
    }

    private String committeeKeyFor(String address) {
        List<String> keys = props.getCommitteePrivateKeys();
        if (keys == null || address == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                if (Credentials.create(normalizeKey(key)).getAddress().equalsIgnoreCase(address)) {
                    return key;
                }
            } catch (Exception ignored) {
                 
            }
        }
        return null;
    }

    public TxInfo createAssetInfo(byte[] metadataRoot, byte[] encryptedRef, BigInteger shardId) {
        return sendInfo(assetRegistry().createAsset(metadataRoot, encryptedRef, shardId));
    }

    public TxInfo formCommitteeInfo(BigInteger epoch) {
        return sendInfo(validator().formCommittee(epoch));
    }

    public TxInfo registerValidatorInfo(BigInteger weiValue) {
        return sendInfo(validator().register(weiValue));
    }

    private static String normalizeKey(String key) {
        return key.startsWith("0x") ? key : "0x" + key;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    public Integer routeToShard(String senderAddress, String receiverAddress) {
        try {
             
            BigInteger shard = getOptimalShard(senderAddress);
            return shard.intValue();
        } catch (Exception e) {
            log.warn("getOptimalShard 调用失败，降级路由到 shard 0：{}", e.getMessage());
            return 0;
        }
    }
}