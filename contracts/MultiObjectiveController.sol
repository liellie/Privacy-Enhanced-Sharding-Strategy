// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./FixedPointMath.sol";

interface IShardRegistryMetrics {
    function loadImbalance() external view returns (uint256);   // f2
    function privacyRisk() external view returns (uint256);     // f3
    function setLambda(uint256 l1, uint256 l2, uint256 l3, uint256 beta) external;
    function lambda1Wad() external view returns (uint256);
    function lambda2Wad() external view returns (uint256);
    function lambda3Wad() external view returns (uint256);
}

interface ICrossShardMetrics {
     
    function crossShardRatioWad() external view returns (uint256);
}

/**
 * @title MultiObjectiveController
 * @dev Updates objective weights from finalized cross-shard, imbalance and budget metrics.
 * The gradient is mean(f)-f; projection enforces the configured simplex floor.
 * beta is lambda2/(lambda1+lambda2). This heuristic does not establish Pareto optimality.
 */
contract MultiObjectiveController {
    uint256 public constant WAD = 1e18;

    address public admin;
    IShardRegistryMetrics public registry;
    ICrossShardMetrics public crossShard;

    uint256 public etaWad;               
    uint256 public lastEpoch;
    uint256 public stepCount;

    uint256 public lambdaMinWad;

    event Stepped(
        uint256 indexed epoch,
        uint256 f1,
        uint256 f2,
        uint256 f3,
        uint256 l1,
        uint256 l2,
        uint256 l3,
        uint256 beta
    );

    modifier onlyAdmin() {
        require(msg.sender == admin, "Controller: not admin");
        _;
    }

    constructor(address _registry, address _crossShard, uint256 _etaWad) {
        require(_etaWad > 0 && _etaWad <= WAD, "Controller: bad eta");
        admin = msg.sender;
        registry = IShardRegistryMetrics(_registry);
        crossShard = ICrossShardMetrics(_crossShard);
        etaWad = _etaWad;
        lambdaMinWad = WAD / 5;          
    }

    function setEta(uint256 _etaWad) external onlyAdmin {
        require(_etaWad > 0 && _etaWad <= WAD, "Controller: bad eta");
        etaWad = _etaWad;
    }

    function setLambdaMin(uint256 _lambdaMinWad) external onlyAdmin {
        require(_lambdaMinWad * 3 < WAD, "Controller: lambda floor too large");
        lambdaMinWad = _lambdaMinWad;
    }

    function objectives() public view returns (uint256 f1, uint256 f2, uint256 f3) {
        f1 = crossShard.crossShardRatioWad();
        f2 = registry.loadImbalance();
        f3 = registry.privacyRisk();
        if (f1 > WAD) f1 = WAD;
        if (f2 > WAD) f2 = WAD;
        if (f3 > WAD) f3 = WAD;
    }

    function step(uint256 epoch) external {
        require(epoch > lastEpoch || stepCount == 0, "Controller: epoch not advanced");

        (uint256 f1, uint256 f2, uint256 f3) = objectives();
        uint256 fbar = (f1 + f2 + f3) / 3;

        int256[3] memory v;
        v[0] = int256(registry.lambda1Wad()) - _etaTimes(int256(fbar) - int256(f1));
        v[1] = int256(registry.lambda2Wad()) - _etaTimes(int256(fbar) - int256(f2));
        v[2] = int256(registry.lambda3Wad()) - _etaTimes(int256(fbar) - int256(f3));

        int256 floorWad = int256(lambdaMinWad);
        int256 target = int256(WAD) - 3 * floorWad;
        v[0] -= floorWad;
        v[1] -= floorWad;
        v[2] -= floorWad;

        uint256[3] memory mu = _projectToSimplex(v, target);
        uint256[3] memory lam;
        lam[0] = mu[0] + lambdaMinWad;
        lam[1] = mu[1] + lambdaMinWad;
        lam[2] = mu[2] + lambdaMinWad;

        uint256 denom = lam[0] + lam[1];
        uint256 beta = denom == 0 ? WAD / 2 : (lam[1] * WAD) / denom;

        registry.setLambda(lam[0], lam[1], lam[2], beta);

        lastEpoch = epoch;
        stepCount += 1;

        emit Stepped(epoch, f1, f2, f3, lam[0], lam[1], lam[2], beta);
    }

    function _etaTimes(int256 g) internal view returns (int256) {
        return (g * int256(etaWad)) / int256(WAD);
    }

    function _projectToSimplex(int256[3] memory v, int256 target)
        internal
        pure
        returns (uint256[3] memory out)
    {
        require(target > 0, "Controller: bad projection target");
        int256[3] memory u = [v[0], v[1], v[2]];

        if (u[0] < u[1]) (u[0], u[1]) = (u[1], u[0]);
        if (u[1] < u[2]) (u[1], u[2]) = (u[2], u[1]);
        if (u[0] < u[1]) (u[0], u[1]) = (u[1], u[0]);

        int256 cumulative;
        int256 theta;
        uint256 rho;

        for (uint256 j = 1; j <= 3; j++) {
            cumulative += u[j - 1];
            int256 candidate = u[j - 1] + (target - cumulative) / int256(j);
            if (candidate > 0) {
                rho = j;
                theta = (cumulative - target) / int256(j);
            }
        }
        require(rho > 0, "Controller: projection failed");

        uint256 sum;
        for (uint256 i = 0; i < 3; i++) {
            int256 x = v[i] - theta;
            out[i] = x > 0 ? uint256(x) : 0;
            sum += out[i];
        }

        uint256 tgt = uint256(target);
        if (sum == 0) {
            out[0] = tgt / 3;
            out[1] = tgt / 3;
            out[2] = tgt - 2 * (tgt / 3);
            return out;
        }
        for (uint256 i = 0; i < 3; i++) {
            out[i] = (out[i] * tgt) / sum;
        }
        uint256 s2 = out[0] + out[1] + out[2];
        if (s2 < tgt) {
            out[0] += tgt - s2;
        } else if (s2 > tgt) {
            uint256 excess = s2 - tgt;
            if (out[0] >= excess) out[0] -= excess;
            else if (out[1] >= excess) out[1] -= excess;
            else out[2] -= excess;
        }
    }
}
