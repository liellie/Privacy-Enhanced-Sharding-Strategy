// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface ILegacyCrossShard {
     
    function getCrossShardRatio() external view returns (uint256);
}

/**
 * @title CrossShardMetricsAdapter
 * @dev Adapter for registered cross-shard operation counters.
 * Supplies a cost-ratio input to the controller, not a measure of complete atomic asset execution.
 */
contract CrossShardMetricsAdapter {
    uint256 public constant WAD = 1e18;

    ILegacyCrossShard public immutable source;

    constructor(address _source) {
        require(_source != address(0), "Adapter: zero address");
        source = ILegacyCrossShard(_source);
    }

    function crossShardRatioWad() external view returns (uint256) {
        uint256 pct = source.getCrossShardRatio();
        if (pct >= 100) return WAD;
        return (pct * WAD) / 100;
    }
}
