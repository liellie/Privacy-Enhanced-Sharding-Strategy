// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface IValidatorRegistryRO {
    function isCommitteeMember(uint256 epoch, uint256 shardId, address who) external view returns (bool);
    function committeeSize(uint256 epoch, uint256 shardId) external view returns (uint256);
    function committeeFormed(uint256 epoch) external view returns (bool);
}

/**
 * @title CrossShardCoordinator
 * @dev Two-phase coordination prototype with per-object locks, committee votes and timeouts.
 * Commit releases locks and records status; it does not apply asset mutations or verify object versions.
 * Read/write-set agreement and integration with asset-state execution require additional validation.
 */
contract CrossShardCoordinator {

    enum Status { None, Pending, Committed, Aborted }

    struct CrossTx {
        address initiator;
        uint256 epoch;
        uint256 deadlineBlock;
        uint256 startBlock;
        Status status;
        uint256 inputCount;
        uint256 approvedShards;       
        bool rejected;                
    }

    address public admin;
    IValidatorRegistryRO public registry;

    uint256 public timeoutBlocks;
    uint256 public quorumNumerator;       
    uint256 public quorumDenominator;

    mapping(bytes32 => CrossTx) public txs;
     
    mapping(bytes32 => mapping(uint256 => bool)) public shardApproved;
     
    mapping(bytes32 => mapping(uint256 => uint256)) public approvals;
     
    mapping(bytes32 => mapping(uint256 => mapping(address => bool))) public hasVoted;
     
    mapping(bytes32 => uint256[]) private _inputShards;
     
    mapping(uint256 => mapping(bytes32 => bytes32)) public lockedBy;
     
    mapping(bytes32 => mapping(uint256 => bytes32[])) private _lockedObjects;

    uint256 public initiatedCount;
    uint256 public committedCount;
    uint256 public abortedCount;
    uint256 public totalVoteCount;
    uint256 public totalLatencyBlocks;

    event Initiated(bytes32 indexed txId, address indexed initiator, uint256 epoch, uint256 inputCount, uint256 deadlineBlock);
    event Voted(bytes32 indexed txId, uint256 indexed shardId, address indexed voter, bool approve, uint256 tally);
    event ShardPrepared(bytes32 indexed txId, uint256 indexed shardId, uint256 lockedObjects);
    event Committed(bytes32 indexed txId, uint256 latencyBlocks, uint256 votes);
    event Aborted(bytes32 indexed txId, string reason);

    modifier onlyAdmin() {
        require(msg.sender == admin, "XShard: not admin");
        _;
    }

    constructor(address _registry, uint256 _timeoutBlocks, uint256 _num, uint256 _den) {
        require(_den > 0 && _num > 0 && _num <= _den, "XShard: bad quorum");
        require(_timeoutBlocks > 0, "XShard: bad timeout");
        admin = msg.sender;
        registry = IValidatorRegistryRO(_registry);
        timeoutBlocks = _timeoutBlocks;
        quorumNumerator = _num;
        quorumDenominator = _den;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function initiate(bytes32 txId, uint256 epoch, uint256[] calldata inputShards) external {
        require(txs[txId].status == Status.None, "XShard: txId already used");
        require(inputShards.length > 0, "XShard: no input shards");
        require(registry.committeeFormed(epoch), "XShard: committee not formed");

        for (uint256 i = 0; i < inputShards.length; i++) {
            for (uint256 j = i + 1; j < inputShards.length; j++) {
                require(inputShards[i] != inputShards[j], "XShard: duplicate shard");
            }
            _inputShards[txId].push(inputShards[i]);
        }

        txs[txId] = CrossTx({
            initiator: msg.sender,
            epoch: epoch,
            deadlineBlock: block.number + timeoutBlocks,
            startBlock: block.number,
            status: Status.Pending,
            inputCount: inputShards.length,
            approvedShards: 0,
            rejected: false
        });
        initiatedCount += 1;

        emit Initiated(txId, msg.sender, epoch, inputShards.length, block.number + timeoutBlocks);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function quorumOf(uint256 epoch, uint256 shardId) public view returns (uint256) {
        uint256 size = registry.committeeSize(epoch, shardId);
        require(size > 0, "XShard: empty committee");
        return (size * quorumNumerator + quorumDenominator - 1) / quorumDenominator;
    }

    function vote(bytes32 txId, uint256 shardId, bool approve, bytes32[] calldata objects) external {
        CrossTx storage t = txs[txId];
        require(t.status == Status.Pending, "XShard: not pending");
        require(block.number <= t.deadlineBlock, "XShard: past deadline");
        require(_isInputShard(txId, shardId), "XShard: not an input shard");
        require(registry.isCommitteeMember(t.epoch, shardId, msg.sender), "XShard: not committee member");
        require(!hasVoted[txId][shardId][msg.sender], "XShard: already voted");

        hasVoted[txId][shardId][msg.sender] = true;
        totalVoteCount += 1;

        if (!approve) {
            t.rejected = true;
            emit Voted(txId, shardId, msg.sender, false, approvals[txId][shardId]);
            _abort(txId, "shard voted no");
            return;
        }

        approvals[txId][shardId] += 1;
        emit Voted(txId, shardId, msg.sender, true, approvals[txId][shardId]);

        if (shardApproved[txId][shardId]) return;                
        if (approvals[txId][shardId] < quorumOf(t.epoch, shardId)) return;

        for (uint256 i = 0; i < objects.length; i++) {
            bytes32 obj = objects[i];
            bytes32 holder = lockedBy[shardId][obj];
            require(holder == bytes32(0) || holder == txId, "XShard: object already locked");
            if (holder == bytes32(0)) {
                lockedBy[shardId][obj] = txId;
                _lockedObjects[txId][shardId].push(obj);
            }
        }
        shardApproved[txId][shardId] = true;
        t.approvedShards += 1;
        emit ShardPrepared(txId, shardId, objects.length);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function commit(bytes32 txId) external {
        CrossTx storage t = txs[txId];
        require(t.status == Status.Pending, "XShard: not pending");
        require(!t.rejected, "XShard: already rejected");
        require(block.number <= t.deadlineBlock, "XShard: past deadline");
        require(t.approvedShards == t.inputCount, "XShard: not all shards prepared");

        t.status = Status.Committed;
        _releaseLocks(txId);

        uint256 latency = block.number - t.startBlock;
        committedCount += 1;
        totalLatencyBlocks += latency;
        emit Committed(txId, latency, _countVotes(txId));
    }

    function abort(bytes32 txId) external {
        CrossTx storage t = txs[txId];
        require(t.status == Status.Pending, "XShard: not pending");
        require(
            block.number > t.deadlineBlock || t.rejected || msg.sender == t.initiator,
            "XShard: cannot abort yet"
        );
        _abort(txId, block.number > t.deadlineBlock ? "timeout" : "requested");
    }

    function _abort(bytes32 txId, string memory reason) internal {
        CrossTx storage t = txs[txId];
        t.status = Status.Aborted;
        _releaseLocks(txId);
        abortedCount += 1;
        emit Aborted(txId, reason);
    }

    function _releaseLocks(bytes32 txId) internal {
        uint256[] storage shards = _inputShards[txId];
        for (uint256 i = 0; i < shards.length; i++) {
            uint256 s = shards[i];
            bytes32[] storage objs = _lockedObjects[txId][s];
            for (uint256 j = 0; j < objs.length; j++) {
                if (lockedBy[s][objs[j]] == txId) {
                    lockedBy[s][objs[j]] = bytes32(0);
                }
            }
            delete _lockedObjects[txId][s];
        }
    }

    function _isInputShard(bytes32 txId, uint256 shardId) internal view returns (bool) {
        uint256[] storage shards = _inputShards[txId];
        for (uint256 i = 0; i < shards.length; i++) {
            if (shards[i] == shardId) return true;
        }
        return false;
    }

    function _countVotes(bytes32 txId) internal view returns (uint256 n) {
        uint256[] storage shards = _inputShards[txId];
        for (uint256 i = 0; i < shards.length; i++) {
            n += approvals[txId][shards[i]];
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function inputShardsOf(bytes32 txId) external view returns (uint256[] memory) {
        return _inputShards[txId];
    }

    function lockedObjectsOf(bytes32 txId, uint256 shardId) external view returns (bytes32[] memory) {
        return _lockedObjects[txId][shardId];
    }

    function statistics()
        external
        view
        returns (
            uint256 initiated,
            uint256 committed,
            uint256 aborted,
            uint256 avgLatencyBlocks,
            uint256 avgVotesPerTx
        )
    {
        initiated = initiatedCount;
        committed = committedCount;
        aborted = abortedCount;
        avgLatencyBlocks = committedCount == 0 ? 0 : totalLatencyBlocks / committedCount;
        avgVotesPerTx = initiatedCount == 0 ? 0 : totalVoteCount / initiatedCount;
    }

    function setTimeout(uint256 blocks_) external onlyAdmin {
        require(blocks_ > 0, "XShard: bad timeout");
        timeoutBlocks = blocks_;
    }
}
