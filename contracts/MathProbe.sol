// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./FixedPointMath.sol";
import "./DeterministicLaplace.sol";

contract MathProbe {
    function lnWadPublic(uint256 x) external pure returns (int256) {
        return FixedPointMath.lnWad(x);
    }

    function expNegWadPublic(uint256 x) external pure returns (uint256) {
        return FixedPointMath.expNegWad(x);
    }

    function laplacePublic(
        bytes32 beacon,
        uint8 purpose,
        uint256 subject,
        uint256 nonce,
        uint256 scaleWad
    ) external pure returns (int256) {
        return DeterministicLaplace.sample(beacon, purpose, subject, nonce, scaleWad);
    }

    function perturbPublic(
        uint256 valueWad,
        bytes32 beacon,
        uint8 purpose,
        uint256 subject,
        uint256 nonce,
        uint256 scaleWad
    ) external pure returns (uint256) {
        return DeterministicLaplace.perturbNonNegative(
            valueWad,
            beacon,
            purpose,
            subject,
            nonce,
            scaleWad
        );
    }
}
