// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./FixedPointMath.sol";

/**
 * @title DeterministicLaplace
 * @dev Deterministic fixed-point Laplace sampling from an epoch beacon and purpose-separated inputs.
 * Replicas can reproduce the same sample. A public beacon is not a private randomness source.
 */
library DeterministicLaplace {
    uint256 internal constant WAD = 1e18;

    uint8 internal constant PURPOSE_LOAD = 1;
    uint8 internal constant PURPOSE_AFFINITY = 2;

    function sample(
        bytes32 beacon,
        uint8 purpose,
        uint256 subject,
        uint256 nonce,
        uint256 scaleWad
    ) internal pure returns (int256) {
        if (scaleWad == 0) return 0;

        bytes32 h = keccak256(abi.encodePacked(beacon, purpose, subject, nonce));

        int256 u = int256(uint256(h) % WAD) - int256(WAD / 2);

        bool negative = u < 0;
        uint256 absU = negative ? uint256(-u) : uint256(u);

        uint256 t = WAD - 2 * absU;
        if (t == 0) t = 1;

        int256 lnT = FixedPointMath.lnWad(t);
        uint256 magnitude = FixedPointMath.mulWad(scaleWad, uint256(-lnT));

        return negative ? -int256(magnitude) : int256(magnitude);
    }

    function perturbNonNegative(
        uint256 valueWad,
        bytes32 beacon,
        uint8 purpose,
        uint256 subject,
        uint256 nonce,
        uint256 scaleWad
    ) internal pure returns (uint256) {
        int256 noise = sample(beacon, purpose, subject, nonce, scaleWad);
        int256 v = int256(valueWad) + noise;
        return v > 0 ? uint256(v) : 0;
    }
}
