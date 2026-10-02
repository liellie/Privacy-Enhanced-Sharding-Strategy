// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title ResourceOracle
 * @dev Committee aggregation of shard resource readings and externally computed predictions.
 * Each dimension is finalized using the median. Forecast fitting occurs off chain.
 */
contract ResourceOracle {
    uint256 public constant SCALE_MAX = 10000;    

    address public admin;
    uint256 public immutable epochLength;

    mapping(address => bool) public isCommitteeMember;
    uint256 public memberCount;
    uint256 public quorum;                        

    struct Reading {
        uint256 cpu;
        uint256 mem;
        uint256 stor;
        uint256 net;
    }

    mapping(uint256 => mapping(uint256 => Reading[])) private _readings;
    mapping(uint256 => mapping(uint256 => uint256[])) private _predictions;
    mapping(uint256 => mapping(uint256 => mapping(address => bool))) public hasSubmitted;

    mapping(uint256 => mapping(uint256 => Reading)) public finalizedReading;
    mapping(uint256 => mapping(uint256 => uint256)) public finalizedPrediction;
    mapping(uint256 => mapping(uint256 => bool)) public isFinalized;

    event MemberAdded(address indexed who);
    event Submitted(uint256 indexed epoch, uint256 indexed shardId, address indexed who);
    event Finalized(
        uint256 indexed epoch,
        uint256 indexed shardId,
        uint256 cpu,
        uint256 mem,
        uint256 stor,
        uint256 net,
        uint256 prediction
    );

    modifier onlyAdmin() {
        require(msg.sender == admin, "Oracle: not admin");
        _;
    }

    constructor(uint256 _epochLength, uint256 _quorum) {
        require(_epochLength > 0, "Oracle: bad epoch length");
        require(_quorum > 0, "Oracle: bad quorum");
        admin = msg.sender;
        epochLength = _epochLength;
        quorum = _quorum;
    }

    function currentEpoch() public view returns (uint256) {
        return block.number / epochLength;
    }

    function addMember(address who) external onlyAdmin {
        require(who != address(0), "Oracle: zero address");
        require(!isCommitteeMember[who], "Oracle: already a member");
        isCommitteeMember[who] = true;
        memberCount += 1;
        emit MemberAdded(who);
    }

    function setQuorum(uint256 q) external onlyAdmin {
        require(q > 0 && q <= memberCount, "Oracle: bad quorum");
        quorum = q;
    }

    function submit(
        uint256 epoch,
        uint256 shardId,
        uint256 cpu,
        uint256 mem,
        uint256 stor,
        uint256 net,
        uint256 prediction
    ) external {
        require(isCommitteeMember[msg.sender], "Oracle: not a member");
        require(!isFinalized[epoch][shardId], "Oracle: already finalized");
        require(!hasSubmitted[epoch][shardId][msg.sender], "Oracle: already submitted");
        require(
            cpu <= SCALE_MAX && mem <= SCALE_MAX && stor <= SCALE_MAX && net <= SCALE_MAX,
            "Oracle: reading out of range"
        );
        require(prediction <= SCALE_MAX, "Oracle: prediction out of range");

        hasSubmitted[epoch][shardId][msg.sender] = true;
        _readings[epoch][shardId].push(Reading(cpu, mem, stor, net));
        _predictions[epoch][shardId].push(prediction);

        emit Submitted(epoch, shardId, msg.sender);
    }

    function submissionCount(uint256 epoch, uint256 shardId) external view returns (uint256) {
        return _readings[epoch][shardId].length;
    }

    function finalizeShard(uint256 epoch, uint256 shardId) external {
        require(!isFinalized[epoch][shardId], "Oracle: already finalized");
        uint256 n = _readings[epoch][shardId].length;
        require(n >= quorum, "Oracle: quorum not reached");

        uint256[] memory cpu = new uint256[](n);
        uint256[] memory mem = new uint256[](n);
        uint256[] memory sto = new uint256[](n);
        uint256[] memory net = new uint256[](n);
        uint256[] memory pre = new uint256[](n);

        for (uint256 i = 0; i < n; i++) {
            Reading storage r = _readings[epoch][shardId][i];
            cpu[i] = r.cpu;
            mem[i] = r.mem;
            sto[i] = r.stor;
            net[i] = r.net;
            pre[i] = _predictions[epoch][shardId][i];
        }

        Reading memory med = Reading(
            _median(cpu),
            _median(mem),
            _median(sto),
            _median(net)
        );
        uint256 medPred = _median(pre);

        finalizedReading[epoch][shardId] = med;
        finalizedPrediction[epoch][shardId] = medPred;
        isFinalized[epoch][shardId] = true;

        emit Finalized(epoch, shardId, med.cpu, med.mem, med.stor, med.net, medPred);
    }

    function _median(uint256[] memory a) internal pure returns (uint256) {
        uint256 n = a.length;
        for (uint256 i = 1; i < n; i++) {
            uint256 key = a[i];
            uint256 j = i;
            while (j > 0 && a[j - 1] > key) {
                a[j] = a[j - 1];
                j--;
            }
            a[j] = key;
        }
        if (n % 2 == 1) {
            return a[n / 2];
        }
        return (a[n / 2 - 1] + a[n / 2]) / 2;
    }

    function getFinalizedReading(uint256 epoch, uint256 shardId)
        external
        view
        returns (uint256 cpu, uint256 mem, uint256 stor, uint256 net, uint256 prediction)
    {
        require(isFinalized[epoch][shardId], "Oracle: not finalized");
        Reading storage r = finalizedReading[epoch][shardId];
        return (r.cpu, r.mem, r.stor, r.net, finalizedPrediction[epoch][shardId]);
    }
}
