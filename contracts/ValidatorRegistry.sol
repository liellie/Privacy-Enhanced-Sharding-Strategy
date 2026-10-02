// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface IEpochBeaconRO {
    function beacon(uint256 epoch) external view returns (bytes32);
    function isFinalized(uint256 epoch) external view returns (bool);
    function currentEpoch() external view returns (uint256);
}

/**
 * @title ValidatorRegistry
 * @dev Validator registration, staking and beacon-based shard sortition.
 * Validator committees are separate from organizational affinity groups.
 * Hash sortition can produce empty committees; security depends on membership and adversary assumptions.
 */
contract ValidatorRegistry {
    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    address public admin;
    IEpochBeaconRO public beaconContract;

    uint256 public shardCount;
    uint256 public minStake;
    uint256 public unbondingEpochs;       

    struct Validator {
        uint256 stake;
        uint256 joinEpoch;                
        uint256 exitEpoch;                
        bool registered;
        bool slashed;
    }

    mapping(address => Validator) public validators;
    address[] public validatorList;

    mapping(uint256 => mapping(uint256 => address[])) private _committee;
    mapping(uint256 => bool) public committeeFormed;

    event ValidatorRegistered(address indexed who, uint256 stake, uint256 joinEpoch);
    event ValidatorExitRequested(address indexed who, uint256 exitEpoch);
    event ValidatorWithdrawn(address indexed who, uint256 stake);
    event ValidatorSlashed(address indexed who, uint256 stake);
    event CommitteeFormed(uint256 indexed epoch, uint256 totalActive);

    modifier onlyAdmin() {
        require(msg.sender == admin, "Validator: not admin");
        _;
    }

    constructor(address _beacon, uint256 _shardCount, uint256 _minStake, uint256 _unbondingEpochs) {
        require(_shardCount > 0, "Validator: bad shard count");
        admin = msg.sender;
        beaconContract = IEpochBeaconRO(_beacon);
        shardCount = _shardCount;
        minStake = _minStake;
        unbondingEpochs = _unbondingEpochs;
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function register() external payable {
        require(!validators[msg.sender].registered, "Validator: already registered");
        require(msg.value >= minStake, "Validator: stake below minimum");

        uint256 joinAt = beaconContract.currentEpoch() + 1;
        validators[msg.sender] = Validator({
            stake: msg.value,
            joinEpoch: joinAt,
            exitEpoch: 0,
            registered: true,
            slashed: false
        });
        validatorList.push(msg.sender);
        emit ValidatorRegistered(msg.sender, msg.value, joinAt);
    }

    function requestExit() external {
        Validator storage v = validators[msg.sender];
        require(v.registered && !v.slashed, "Validator: not active");
        require(v.exitEpoch == 0, "Validator: exit already requested");
        v.exitEpoch = beaconContract.currentEpoch() + 1;
        emit ValidatorExitRequested(msg.sender, v.exitEpoch);
    }

    function withdraw() external {
        Validator storage v = validators[msg.sender];
        require(v.registered, "Validator: not registered");
        require(v.exitEpoch != 0, "Validator: exit not requested");
        require(!v.slashed, "Validator: slashed");
        require(
            beaconContract.currentEpoch() >= v.exitEpoch + unbondingEpochs,
            "Validator: still unbonding"
        );

        uint256 amount = v.stake;
        v.stake = 0;
        v.registered = false;
        (bool ok, ) = payable(msg.sender).call{value: amount}("");
        require(ok, "Validator: transfer failed");
        emit ValidatorWithdrawn(msg.sender, amount);
    }

    function slash(address who) external onlyAdmin {
        Validator storage v = validators[who];
        require(v.registered && !v.slashed, "Validator: not active");
        uint256 amount = v.stake;
        v.stake = 0;
        v.slashed = true;
        emit ValidatorSlashed(who, amount);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function isActiveAt(address who, uint256 epoch) public view returns (bool) {
        Validator storage v = validators[who];
        if (!v.registered || v.slashed) return false;
        if (epoch < v.joinEpoch) return false;
        if (v.exitEpoch != 0 && epoch >= v.exitEpoch) return false;
        return true;
    }

    function shardOf(address who, uint256 epoch) public view returns (uint256) {
        bytes32 b = beaconContract.beacon(epoch);
        return uint256(keccak256(abi.encodePacked(b, who))) % shardCount;
    }

    function formCommittee(uint256 epoch) external {
        require(!committeeFormed[epoch], "Validator: already formed");
        require(beaconContract.isFinalized(epoch), "Validator: beacon not finalized");

        uint256 active;
        for (uint256 i = 0; i < validatorList.length; i++) {
            address v = validatorList[i];
            if (!isActiveAt(v, epoch)) continue;
            _committee[epoch][shardOf(v, epoch)].push(v);
            active += 1;
        }
        require(active >= shardCount, "Validator: not enough active validators");

        committeeFormed[epoch] = true;
        emit CommitteeFormed(epoch, active);
    }

    function committeeOf(uint256 epoch, uint256 shardId) external view returns (address[] memory) {
        return _committee[epoch][shardId];
    }

    function committeeSize(uint256 epoch, uint256 shardId) external view returns (uint256) {
        return _committee[epoch][shardId].length;
    }

    function isCommitteeMember(uint256 epoch, uint256 shardId, address who)
        external
        view
        returns (bool)
    {
        address[] storage c = _committee[epoch][shardId];
        for (uint256 i = 0; i < c.length; i++) {
            if (c[i] == who) return true;
        }
        return false;
    }

    function activeValidatorCount(uint256 epoch) public view returns (uint256 n) {
        for (uint256 i = 0; i < validatorList.length; i++) {
            if (isActiveAt(validatorList[i], epoch)) n += 1;
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function requiredShardSize(uint256 maliciousPct, uint256 lambda)
        public
        pure
        returns (uint256)
    {
        require(maliciousPct > 0 && maliciousPct < 50, "Validator: pct out of range");
        uint256 dScaled = _klDivergence(maliciousPct);   // D(1/2 || p) * 1e6
        require(dScaled > 0, "Validator: divergence too small");
        // m >= lambda * ln2 / D ；ln2 * 1e6 = 693147
        return (lambda * 693147 + dScaled - 1) / dScaled;
    }

    function _klDivergence(uint256 p) internal pure returns (uint256) {
         
        if (p <= 10) return 255412;
        if (p <= 15) return 155146;
        if (p <= 20) return 91161;
        if (p <= 25) return 49041;
        if (p <= 30) return 20135;
        if (p <= 33) return 8109;
        return 1;
    }

    // ------------------------------------------------------------------

    function setShardCount(uint256 k) external onlyAdmin {
        require(k > 0, "Validator: bad shard count");
        shardCount = k;
    }

    function validatorCount() external view returns (uint256) {
        return validatorList.length;
    }
}
