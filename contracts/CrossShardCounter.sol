// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title CrossShardCounter
 * @dev Counts registered intra-shard and cross-shard operations.
 * These counters describe reported classifications, not execution latency or asset atomicity.
 */
contract CrossShardCounter {
    uint256 public constant WAD = 1e18;

    address public admin;
    address public recorder;

    uint256 public crossShardOps;
    uint256 public totalOps;

    event OperationRecorded(bool isCrossShard, uint256 crossShardOps, uint256 totalOps);
    event CountersReset();

    modifier onlyAdmin() {
        require(msg.sender == admin, "Counter: not admin");
        _;
    }

    constructor() {
        admin = msg.sender;
        recorder = msg.sender;
    }

    function setRecorder(address who) external onlyAdmin {
        recorder = who;
    }

    function record(bool isCrossShard) external {
        require(msg.sender == recorder || msg.sender == admin, "Counter: not recorder");
        totalOps += 1;
        if (isCrossShard) {
            crossShardOps += 1;
        }
        emit OperationRecorded(isCrossShard, crossShardOps, totalOps);
    }

    function recordBatch(uint256 crossCount, uint256 total) external {
        require(msg.sender == recorder || msg.sender == admin, "Counter: not recorder");
        require(crossCount <= total, "Counter: cross exceeds total");
        crossShardOps += crossCount;
        totalOps += total;
        emit OperationRecorded(crossCount > 0, crossShardOps, totalOps);
    }

    function crossShardRatioWad() external view returns (uint256) {
        if (totalOps == 0) return 0;
        return (crossShardOps * WAD) / totalOps;
    }

    function reset() external onlyAdmin {
        crossShardOps = 0;
        totalOps = 0;
        emit CountersReset();
    }
}
