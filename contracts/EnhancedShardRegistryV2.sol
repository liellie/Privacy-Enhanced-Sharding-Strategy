// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./FixedPointMath.sol";
import "./DeterministicLaplace.sol";

interface IEpochBeacon {
    function beacon(uint256 epoch) external view returns (bytes32);
    function currentEpoch() external view returns (uint256);
    function isFinalized(uint256 epoch) external view returns (bool);
}

interface IResourceOracle {
    function getFinalizedReading(uint256 epoch, uint256 shardId)
        external view returns (uint256 cpu, uint256 mem, uint256 stor, uint256 net, uint256 prediction);
    function isFinalized(uint256 epoch, uint256 shardId) external view returns (bool);
    function SCALE_MAX() external view returns (uint256);
}

interface IAffinityManager {
    function primaryShardOf(address a) external view returns (bool hasShard, uint256 shardId);
    function groupOf(address a) external view returns (uint256);
}

/**
 * @title EnhancedShardRegistryV2
 * @dev Epoch configuration and deterministic address selection prototype.
 * syncShard, adjustResourceWeights and publishPrivatizedLoad update epoch state.
 * View selectors read cached state; the offline replay additionally tracks within-epoch assignments.
 * Resource normalization uses SCALE_MAX, histories are per shard, and exhausted quotas reuse cached releases.
 * Public-beacon noise is reproducible and does not hide values from full nodes.
 */
contract EnhancedShardRegistryV2 {
    using FixedPointMath for uint256;

    uint256 public constant WAD = 1e18;
    uint256 public constant DIMS = 4;

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    address public admin;
    IEpochBeacon public beaconContract;
    IResourceOracle public oracle;
    IAffinityManager public affinity;
    address public controller;              // MultiObjectiveController

    struct ResourceVector {
        uint256 cpu;     
        uint256 mem;
        uint256 stor;
        uint256 net;
    }

    struct ShardInfo {
        uint256 id;
        string name;
        address shardContract;
        ResourceVector resources;
        uint256 loadWad;                    // Eq.(4)
        uint256 predictedLoadWad;
        uint256 finalLoadWad;               // Eq.(6)
        uint256 organizationCount;
        bool active;
         
        uint256 budgetWad;                  // B_i
        uint256 lastPublishEpoch;
        uint256 publishedLoadWad;            
        bool hasPublished;
        uint256 publishNonce;
    }

    mapping(uint256 => ShardInfo) private _shards;
    uint256 public shardCount;

    uint256[DIMS] public weightsWad;
    uint256 public weightFloorWad;           

    // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
    uint256 public historyWindow;
    mapping(uint256 => uint256[]) private _historyWad;
    mapping(uint256 => uint256) private _historyIdx;

    struct PredictionStats {
        uint256 count;
        uint256 sumAbsErrWad;
        uint256 sumSqErrWad;
        uint256 sumActualWad;
    }
    mapping(uint256 => PredictionStats) public predictionStats;

    uint256 public epsilonBaseWad;          // Eq.(16) epsilon_base
    uint256 public rhoWad;                   
    uint256 public omegaWad;                 
    uint256 public gammaWad;                 
    uint256 public budgetMaxWad;            // B_max
    uint256 public sensitivityLoadWad;      // Eq.(8) Delta L
    uint256 public sensitivityAffinityWad;  // Eq.(9) Delta A
    uint256 public epsilon2BaseWad;          
    mapping(uint256 => uint256) public affinityBudgetWad;    
    mapping(uint256 => uint256) public affinityNonce;
    mapping(uint256 => uint256) public lastLocalityBenefitWad;
    mapping(uint256 => bool) public hasLocalityBenefit;

    uint256 public alphaBaseWad;            // Eq.(7) alpha_base
    uint256 public kappaWad;                 

    uint256 public lambda1Wad;               
    uint256 public lambda2Wad;               
    uint256 public lambda3Wad;               
    uint256 public betaWad;                 // Eq.(19)
    uint256 public thetaBaseWad;             

    event ShardCreated(uint256 indexed shardId, string name);
    event ShardSynced(uint256 indexed shardId, uint256 epoch, uint256 loadWad, uint256 finalLoadWad);
    event WeightsAdjusted(uint256 w0, uint256 w1, uint256 w2, uint256 w3);
    event LoadPublished(uint256 indexed shardId, uint256 epoch, uint256 publishedLoadWad, bool budgetExhausted);
    event LambdaUpdated(uint256 l1, uint256 l2, uint256 l3, uint256 beta);

    modifier onlyAdmin() {
        require(msg.sender == admin, "Registry: not admin");
        _;
    }

    constructor(address _beacon, address _oracle, address _affinity) {
        admin = msg.sender;
        beaconContract = IEpochBeacon(_beacon);
        oracle = IResourceOracle(_oracle);
        affinity = IAffinityManager(_affinity);

        weightsWad = [WAD / 4, WAD / 4, WAD / 4, WAD / 4];
        weightFloorWad = WAD / 20;          // 5%
        historyWindow = 10;

        // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
        epsilonBaseWad = 2 * WAD / 10;
        rhoWad = WAD;
        omegaWad = WAD;
        gammaWad = WAD / 10;
        budgetMaxWad = 100 * WAD;
        sensitivityLoadWad = WAD / 100;      
        sensitivityAffinityWad = WAD / 2;   // Eq.(9) Delta A

        epsilon2BaseWad = 8 * WAD / 10;

        alphaBaseWad = 3 * WAD / 10;
        kappaWad = WAD;

        lambda1Wad = WAD / 3;
        lambda2Wad = WAD / 3;
        lambda3Wad = WAD - 2 * (WAD / 3);
        betaWad = WAD / 2;
        thetaBaseWad = WAD / 5;             // 0.2
    }

    function setController(address c) external onlyAdmin {
        controller = c;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function createShard(string memory name, address shardContract) external onlyAdmin {
        uint256 id = shardCount;
        ShardInfo storage s = _shards[id];
        s.id = id;
        s.name = name;
        s.shardContract = shardContract;
        s.active = true;
        s.budgetWad = budgetMaxWad;
        s.lastPublishEpoch = beaconContract.currentEpoch();

        _historyWad[id] = new uint256[](historyWindow);
        shardCount += 1;

        emit ShardCreated(id, name);
    }

    function deactivateShard(uint256 id) external onlyAdmin {
        require(id < shardCount, "Registry: bad shard");
        _shards[id].active = false;
    }

    function getShard(uint256 id)
        external
        view
        returns (
            uint256 loadWad,
            uint256 predictedLoadWad,
            uint256 finalLoadWad,
            uint256 publishedLoadWad,
            uint256 budgetWad,
            bool active
        )
    {
        ShardInfo storage s = _shards[id];
        return (s.loadWad, s.predictedLoadWad, s.finalLoadWad, s.publishedLoadWad, s.budgetWad, s.active);
    }

    function getResources(uint256 id) external view returns (uint256, uint256, uint256, uint256) {
        ResourceVector storage r = _shards[id].resources;
        return (r.cpu, r.mem, r.stor, r.net);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function _normalize(uint256 raw, uint256 scaleMax) internal pure returns (uint256) {
        if (raw >= scaleMax) return WAD;
        return (raw * WAD) / scaleMax;
    }

    /// @dev Eq.(4)：L_i = sum_j r_j * w_j
    function _weightedLoad(ResourceVector memory r) internal view returns (uint256) {
        return
            FixedPointMath.mulWad(r.cpu, weightsWad[0]) +
            FixedPointMath.mulWad(r.mem, weightsWad[1]) +
            FixedPointMath.mulWad(r.stor, weightsWad[2]) +
            FixedPointMath.mulWad(r.net, weightsWad[3]);
    }

    function syncShard(uint256 epoch, uint256 shardId) external {
        require(shardId < shardCount, "Registry: bad shard");
        ShardInfo storage s = _shards[shardId];
        require(s.active, "Registry: shard inactive");
        require(oracle.isFinalized(epoch, shardId), "Registry: oracle not finalized");

        (ResourceVector memory rv, uint256 predNorm) = _readNormalized(epoch, shardId);
        uint256 newLoad = _weightedLoad(rv);

        if (s.predictedLoadWad > 0) {
            _accumulateError(shardId, s.predictedLoadWad, newLoad);
        }

        s.resources = rv;
        s.loadWad = newLoad;
        s.predictedLoadWad = predNorm;

        // Eq.(6)：L_final = (1 - alpha) * L + alpha * L_hat
        uint256 a = alphaOf(shardId);
        s.finalLoadWad =
            FixedPointMath.mulWad(WAD - a, newLoad) +
            FixedPointMath.mulWad(a, predNorm);

        // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
        uint256[] storage h = _historyWad[shardId];
        h[_historyIdx[shardId]] = newLoad;
        _historyIdx[shardId] = (_historyIdx[shardId] + 1) % historyWindow;

        emit ShardSynced(shardId, epoch, newLoad, s.finalLoadWad);
    }

    function _readNormalized(uint256 epoch, uint256 shardId)
        internal
        view
        returns (ResourceVector memory rv, uint256 predNorm)
    {
        uint256 scaleMax = oracle.SCALE_MAX();
        (uint256 cpu, uint256 mem, uint256 stor, uint256 net, uint256 pred) =
            oracle.getFinalizedReading(epoch, shardId);
        rv = ResourceVector(
            _normalize(cpu, scaleMax),
            _normalize(mem, scaleMax),
            _normalize(stor, scaleMax),
            _normalize(net, scaleMax)
        );
        predNorm = _normalize(pred, scaleMax);
    }

    function _accumulateError(uint256 shardId, uint256 predictedWad, uint256 actualWad) internal {
        PredictionStats storage ps = predictionStats[shardId];
        uint256 err = predictedWad > actualWad ? predictedWad - actualWad : actualWad - predictedWad;
        ps.count += 1;
        ps.sumAbsErrWad += err;
        ps.sumSqErrWad += FixedPointMath.mulWad(err, err);
        ps.sumActualWad += actualWad;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function alphaOf(uint256 shardId) public view returns (uint256) {
        PredictionStats storage ps = predictionStats[shardId];
        if (ps.count == 0) return alphaBaseWad;

        uint256 meanSq = ps.sumSqErrWad / ps.count;              // E[e^2]
        uint256 meanAbs = ps.sumAbsErrWad / ps.count;            // E[|e|]
        uint256 meanActual = ps.sumActualWad / ps.count;
        if (meanActual == 0) return alphaBaseWad;

        uint256 rmse = FixedPointMath.sqrtWad(meanSq);
        uint256 nrmse = FixedPointMath.divWad(rmse, meanActual);

        uint256 meanAbsSq = FixedPointMath.mulWad(meanAbs, meanAbs);
        uint256 variance = meanSq > meanAbsSq ? meanSq - meanAbsSq : 0;

        uint256 decay = FixedPointMath.expNegWad(FixedPointMath.mulWad(kappaWad, variance));

        uint256 factor;
        if (nrmse == 0) {
            factor = decay;                                        
        } else {
            factor = FixedPointMath.mulWad(FixedPointMath.divWad(WAD, nrmse), decay);
        }
        if (factor > WAD) factor = WAD;

        return FixedPointMath.mulWad(alphaBaseWad, factor);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    /** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
    function adjustResourceWeights() external returns (uint256[DIMS] memory) {
        require(shardCount > 0, "Registry: no shards");

        uint256[DIMS] memory g;
        uint256 activeCount;
        for (uint256 i = 0; i < shardCount; i++) {
            if (!_shards[i].active) continue;
            ResourceVector storage r = _shards[i].resources;
            g[0] += r.cpu;
            g[1] += r.mem;
            g[2] += r.stor;
            g[3] += r.net;
            activeCount += 1;
        }
        require(activeCount > 0, "Registry: no active shard");
        for (uint256 d = 0; d < DIMS; d++) {
            g[d] = g[d] / activeCount;
        }

        uint256 j = 0;
        for (uint256 d = 1; d < DIMS; d++) {
            if (g[d] > g[j]) j = d;
        }

        uint256 wOld = weightsWad[j];
        uint256 headroom = WAD - wOld;
        uint256 dw = headroom < WAD / 10 ? headroom : WAD / 10;

        if (dw > 0 && headroom > 0) {
            weightsWad[j] = wOld + dw;
            uint256 num = headroom - dw;           // 1 - w_old - dw
            for (uint256 k = 0; k < DIMS; k++) {
                if (k == j) continue;
                weightsWad[k] = (weightsWad[k] * num) / headroom;
            }
            _applyFloorAndRenormalize(j);
        }

        emit WeightsAdjusted(weightsWad[0], weightsWad[1], weightsWad[2], weightsWad[3]);
        return weightsWad;
    }

    function _applyFloorAndRenormalize(uint256 bottleneck) internal {
        uint256 deficit;
        for (uint256 k = 0; k < DIMS; k++) {
            if (k == bottleneck) continue;
            if (weightsWad[k] < weightFloorWad) {
                deficit += weightFloorWad - weightsWad[k];
                weightsWad[k] = weightFloorWad;
            }
        }
        if (deficit > 0) {
            require(weightsWad[bottleneck] > deficit, "Registry: floor too high");
            weightsWad[bottleneck] -= deficit;
        }

        uint256 sum;
        for (uint256 k = 0; k < DIMS; k++) sum += weightsWad[k];
        if (sum < WAD) {
            weightsWad[bottleneck] += (WAD - sum);
        } else if (sum > WAD) {
            weightsWad[bottleneck] -= (sum - WAD);
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function adaptiveEpsilon(uint256 shardId) public view returns (uint256) {
        ShardInfo storage s = _shards[shardId];
        uint256 loss = budgetMaxWad > s.budgetWad ? budgetMaxWad - s.budgetWad : 0;
        uint256 ratio = FixedPointMath.divWad(loss, budgetMaxWad);
        uint256 denom = WAD + FixedPointMath.mulWad(rhoWad, ratio);
        return FixedPointMath.divWad(epsilonBaseWad, denom);
    }

    function publishPrivatizedLoad(uint256 epoch, uint256 shardId) external {
        require(shardId < shardCount, "Registry: bad shard");
        ShardInfo storage s = _shards[shardId];
        require(s.active, "Registry: shard inactive");
        require(beaconContract.isFinalized(epoch), "Registry: beacon not finalized");
        require(epoch > s.lastPublishEpoch || !s.hasPublished, "Registry: already published");

        uint256 elapsed = epoch > s.lastPublishEpoch ? epoch - s.lastPublishEpoch : 0;
        if (elapsed > 1) {
            uint256 recover = FixedPointMath.mulWad(
                gammaWad,
                FixedPointMath.divWad(WAD, FixedPointMath.sqrtWad(elapsed * WAD))
            );
            s.budgetWad = s.budgetWad + recover > budgetMaxWad ? budgetMaxWad : s.budgetWad + recover;
        }

        uint256 eps = adaptiveEpsilon(shardId);
        uint256 cost = FixedPointMath.mulWad(eps, omegaWad);   // Eq.(14)

        if (s.budgetWad >= cost && eps > 0) {
             
            uint256 scale = FixedPointMath.divWad(sensitivityLoadWad, eps);
            s.publishedLoadWad = DeterministicLaplace.perturbNonNegative(
                s.finalLoadWad,
                beaconContract.beacon(epoch),
                DeterministicLaplace.PURPOSE_LOAD,
                shardId,
                s.publishNonce,
                scale
            );
            s.publishNonce += 1;
            s.budgetWad -= cost;
            s.hasPublished = true;
            s.lastPublishEpoch = epoch;
            emit LoadPublished(shardId, epoch, s.publishedLoadWad, false);
        } else {
             
            if (!s.hasPublished) {
                s.publishedLoadWad = s.finalLoadWad;
                s.hasPublished = true;
            }
            s.lastPublishEpoch = epoch;
            emit LoadPublished(shardId, epoch, s.publishedLoadWad, true);
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    struct LoadStats {
        uint256 minLoad;
        uint256 maxLoad;
        uint256 avgLoad;
        uint256 activeCount;
        bool any;
    }

    function _loadStats() internal view returns (LoadStats memory st) {
        st.minLoad = type(uint256).max;
        uint256 sum;
        for (uint256 i = 0; i < shardCount; i++) {
            ShardInfo storage s = _shards[i];
            if (!s.active || !s.hasPublished) continue;
            uint256 l = s.publishedLoadWad;
            if (l < st.minLoad) st.minLoad = l;
            if (l > st.maxLoad) st.maxLoad = l;
            sum += l;
            st.activeCount += 1;
        }
        st.any = st.activeCount > 0;
        // Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
        if (!st.any) {
            st.minLoad = 0;
            return st;
        }
        st.avgLoad = sum / st.activeCount;
    }

    /** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
    function getOptimalShard(address org) external view returns (uint256) {
        LoadStats memory st = _loadStats();
        require(st.any, "Registry: no published shard");

        (bool hasShard, uint256 affShard) = affinity.primaryShardOf(org);

        if (hasShard && affShard < shardCount && _shards[affShard].active && _shards[affShard].hasPublished) {
            uint256 threshold = _affinityThreshold(st, affinity.groupOf(org));
            if (_shards[affShard].publishedLoadWad <= threshold) {
                return affShard;
            }
        }
        return _enhancedLoadBalancing(st, org);
    }

    function _perturbedLocalityBenefit(uint256 groupId) internal view returns (uint256) {
        if (groupId == 0) return WAD;
        uint256 remaining = affinityBudgetWad[groupId];
        if (remaining == 0 && !hasLocalityBenefit[groupId]) {
            remaining = budgetMaxWad;
        }
        uint256 eps = epsilon2BaseWad;
        uint256 cost = FixedPointMath.mulWad(eps, omegaWad);
        if (remaining < cost || eps == 0) {
             
            return hasLocalityBenefit[groupId] ? lastLocalityBenefitWad[groupId] : WAD;
        }
        uint256 scale = FixedPointMath.divWad(sensitivityAffinityWad, eps);
        uint256 epoch = beaconContract.currentEpoch();
        return DeterministicLaplace.perturbNonNegative(
            WAD,
            beaconContract.beacon(epoch),
            DeterministicLaplace.PURPOSE_AFFINITY,
            groupId,
            affinityNonce[groupId],
            scale
        );
    }

    function settleAffinityBudget(uint256 groupId) external {
        require(msg.sender == controller || msg.sender == admin, "Registry: not controller");
        uint256 remaining = affinityBudgetWad[groupId];
        if (remaining == 0 && !hasLocalityBenefit[groupId]) {
            remaining = budgetMaxWad;
        }
        uint256 cost = FixedPointMath.mulWad(epsilon2BaseWad, omegaWad);
        if (remaining >= cost) {
            affinityBudgetWad[groupId] = remaining - cost;
            affinityNonce[groupId] += 1;
            lastLocalityBenefitWad[groupId] = _perturbedLocalityBenefit(groupId);
            hasLocalityBenefit[groupId] = true;
        }
    }

    function _affinityThreshold(LoadStats memory st, uint256 groupId) internal view returns (uint256) {
        // loadDifference = (L_max - L_min) / L_max
        uint256 loadDiff = st.maxLoad == 0 ? 0 : FixedPointMath.divWad(st.maxLoad - st.minLoad, st.maxLoad);

        uint256 balanceTerm = WAD - FixedPointMath.mulWad(betaWad, loadDiff);
        // Perturb the affinity margin using its independent quota; this is not a relationship-privacy guarantee.
        uint256 localityBenefit = _perturbedLocalityBenefit(groupId);
         
        uint256 privacyFactor = _averageBudgetRatio();

        uint256 bracket =
            FixedPointMath.mulWad(balanceTerm, lambda2Wad) +
            FixedPointMath.mulWad(localityBenefit, lambda1Wad) +
            FixedPointMath.mulWad(privacyFactor, lambda3Wad);

        uint256 factor = WAD + FixedPointMath.mulWad(thetaBaseWad, bracket);
        return FixedPointMath.mulWad(st.minLoad, factor);
    }

    function _averageBudgetRatio() internal view returns (uint256) {
        uint256 sum;
        uint256 n;
        for (uint256 i = 0; i < shardCount; i++) {
            if (!_shards[i].active) continue;
            sum += FixedPointMath.divWad(_shards[i].budgetWad, budgetMaxWad);
            n += 1;
        }
        return n == 0 ? 0 : sum / n;
    }

    function enhancedLoadBalancing(address org) external view returns (uint256) {
        LoadStats memory st = _loadStats();
        require(st.any, "Registry: no published shard");
        return _enhancedLoadBalancing(st, org);
    }

    function _enhancedLoadBalancing(LoadStats memory st, address org) internal view returns (uint256) {
        (bool hasShard, uint256 affShard) = affinity.primaryShardOf(org);

        uint256 best = type(uint256).max;
        uint256 chosen;
        bool found;

        for (uint256 i = 0; i < shardCount; i++) {
            ShardInfo storage s = _shards[i];
            if (!s.active || !s.hasPublished) continue;

            uint256 loadScore = s.publishedLoadWad;
             
            uint256 localityScore = (hasShard && i == affShard) ? 0 : WAD;
             
            uint256 budgetRatio = FixedPointMath.divWad(s.budgetWad, budgetMaxWad);
            uint256 privacyScore = WAD - (budgetRatio > WAD ? WAD : budgetRatio);

            uint256 composite =
                FixedPointMath.mulWad(lambda1Wad, localityScore) +
                FixedPointMath.mulWad(lambda2Wad, loadScore) +
                FixedPointMath.mulWad(lambda3Wad, privacyScore);

            if (!found || composite < best) {
                best = composite;
                chosen = i;
                found = true;
            }
        }
        require(found, "Registry: no candidate shard");
        return chosen;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function setLambda(uint256 l1, uint256 l2, uint256 l3, uint256 beta) external {
        require(msg.sender == controller || msg.sender == admin, "Registry: not controller");
        require(l1 + l2 + l3 == WAD, "Registry: lambda must sum to one");
        require(beta <= WAD, "Registry: beta out of range");
        lambda1Wad = l1;
        lambda2Wad = l2;
        lambda3Wad = l3;
        betaWad = beta;
        emit LambdaUpdated(l1, l2, l3, beta);
    }

    function loadImbalance() external view returns (uint256) {
        LoadStats memory st = _loadStats();
        if (!st.any || st.avgLoad == 0) return 0;
        return FixedPointMath.divWad(st.maxLoad - st.minLoad, st.avgLoad);
    }

    function privacyRisk() external view returns (uint256) {
        return WAD - _averageBudgetRatio();
    }
}
