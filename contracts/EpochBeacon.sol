// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title EpochBeacon
 * @dev Commit/reveal epoch beacon with deterministic finalization.
 * Membership timing, missing reveals and epoch ordering are protocol assumptions; see docs/IMPLEMENTATION_SCOPE.md.
 */
contract EpochBeacon {
    uint256 public immutable epochLength;    
    address public admin;

    mapping(address => bool) public isContributor;
    uint256 public contributorCount;
    uint256 public revealThreshold;          

    mapping(uint256 => mapping(address => bytes32)) public commitmentOf;
    mapping(uint256 => mapping(address => bool)) public hasRevealed;
    mapping(uint256 => uint256) public revealCount;
    mapping(uint256 => bytes32) private _accumulator;

    mapping(uint256 => bytes32) public beaconOf;
    mapping(uint256 => bool) public isFinalized;

    event ContributorAdded(address indexed who);
    event Committed(uint256 indexed epoch, address indexed who);
    event Revealed(uint256 indexed epoch, address indexed who, uint256 count);
    event BeaconFinalized(uint256 indexed epoch, bytes32 beacon);

    modifier onlyAdmin() {
        require(msg.sender == admin, "EpochBeacon: not admin");
        _;
    }

    modifier onlyContributor() {
        require(isContributor[msg.sender], "EpochBeacon: not a contributor");
        _;
    }

    constructor(uint256 _epochLength, uint256 _revealThreshold) {
        require(_epochLength > 0, "EpochBeacon: epoch length must be positive");
        require(_revealThreshold > 0, "EpochBeacon: threshold must be positive");
        epochLength = _epochLength;
        revealThreshold = _revealThreshold;
        admin = msg.sender;
    }

    function currentEpoch() public view returns (uint256) {
        return block.number / epochLength;
    }

    function addContributor(address who) external onlyAdmin {
        require(who != address(0), "EpochBeacon: zero address");
        require(!isContributor[who], "EpochBeacon: already a contributor");
        isContributor[who] = true;
        contributorCount += 1;
        emit ContributorAdded(who);
    }

    function setRevealThreshold(uint256 t) external onlyAdmin {
        require(t > 0 && t <= contributorCount, "EpochBeacon: invalid threshold");
        revealThreshold = t;
    }

    /// @param commitment keccak256(abi.encodePacked(secret, msg.sender))
    function commit(uint256 epoch, bytes32 commitment) external onlyContributor {
        require(epoch >= currentEpoch(), "EpochBeacon: epoch already elapsed");
        require(!isFinalized[epoch], "EpochBeacon: epoch finalized");
        require(commitmentOf[epoch][msg.sender] == bytes32(0), "EpochBeacon: already committed");
        require(commitment != bytes32(0), "EpochBeacon: empty commitment");
        commitmentOf[epoch][msg.sender] = commitment;
        emit Committed(epoch, msg.sender);
    }

    function reveal(uint256 epoch, bytes32 secret) external onlyContributor {
        require(!isFinalized[epoch], "EpochBeacon: epoch finalized");
        require(!hasRevealed[epoch][msg.sender], "EpochBeacon: already revealed");
        require(
            commitmentOf[epoch][msg.sender] == keccak256(abi.encodePacked(secret, msg.sender)),
            "EpochBeacon: commitment mismatch"
        );

        hasRevealed[epoch][msg.sender] = true;
         
        _accumulator[epoch] = _accumulator[epoch] ^ secret;
        revealCount[epoch] += 1;

        emit Revealed(epoch, msg.sender, revealCount[epoch]);
    }

    function finalize(uint256 epoch) external {
        require(!isFinalized[epoch], "EpochBeacon: already finalized");
        require(revealCount[epoch] >= revealThreshold, "EpochBeacon: not enough reveals");

        bytes32 b = keccak256(abi.encodePacked(_accumulator[epoch], epoch));
        beaconOf[epoch] = b;
        isFinalized[epoch] = true;

        emit BeaconFinalized(epoch, b);
    }

    function beacon(uint256 epoch) external view returns (bytes32) {
        require(isFinalized[epoch], "EpochBeacon: epoch not finalized");
        return beaconOf[epoch];
    }
}
