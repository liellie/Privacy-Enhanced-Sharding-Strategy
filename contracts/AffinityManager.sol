// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./FixedPointMath.sol";

/**
 * @title AffinityManager
 * @dev Interaction counts, threshold-based grouping and group placement.
 * Group IDs start at one; zero denotes an ungrouped address.
 * Incremental unions do not split groups when affinity declines.
 */
contract AffinityManager {
    using FixedPointMath for uint256;

    uint256 public constant WAD = 1e18;

    address public admin;
    address public recorder;             

    uint256 public thetaA;

    mapping(bytes32 => uint256) public pairInteractions;   // l(oi, oj)
    mapping(address => uint256) public totalInteractions;  // l(oi)

    mapping(address => address) private _parent;
    mapping(address => bool) public isKnown;

    mapping(uint256 => uint256) public groupVolume;       
    uint256 public lastRebalanceEpoch;

    mapping(address => uint256) private _groupIdOfRoot;
    mapping(uint256 => uint256) public groupMemberCount;
    mapping(uint256 => uint256) public groupPrimaryShard;
    mapping(uint256 => bool) public groupHasShard;
    uint256 public groupCount;

    event InteractionRecorded(address indexed a, address indexed b, uint256 pairCount);
    event EdgeFormed(address indexed a, address indexed b, uint256 affinityWad);
    event GroupsMerged(uint256 indexed survivingGroup, uint256 indexed absorbedGroup);
    event GroupCreated(uint256 indexed groupId, address indexed root);
    event GroupShardAssigned(uint256 indexed groupId, uint256 indexed shardId);
    event ThetaUpdated(uint256 thetaA);

    modifier onlyAdmin() {
        require(msg.sender == admin, "Affinity: not admin");
        _;
    }

    modifier onlyRecorder() {
        require(msg.sender == recorder || msg.sender == admin, "Affinity: not recorder");
        _;
    }

    constructor(uint256 _thetaAWad) {
        require(_thetaAWad > 0 && _thetaAWad <= WAD, "Affinity: theta out of range");
        admin = msg.sender;
        thetaA = _thetaAWad;
    }

    function setRecorder(address _recorder) external onlyAdmin {
        recorder = _recorder;
    }

    function setThetaA(uint256 _thetaAWad) external onlyAdmin {
        require(_thetaAWad > 0 && _thetaAWad <= WAD, "Affinity: theta out of range");
        thetaA = _thetaAWad;
        emit ThetaUpdated(_thetaAWad);
    }

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function pairKey(address a, address b) public pure returns (bytes32) {
        return a < b
            ? keccak256(abi.encodePacked(a, b))
            : keccak256(abi.encodePacked(b, a));
    }

    function affinity(address a, address b) public view returns (uint256) {
        uint256 lab = pairInteractions[pairKey(a, b)];
        if (lab == 0) return 0;

        uint256 la = totalInteractions[a];
        uint256 lb = totalInteractions[b];
        if (la == 0 || lb == 0) return 0;

        uint256 denom = FixedPointMath.sqrt(la * lb);
        if (denom == 0) return 0;

        return (lab * WAD) / denom;
    }

    function recordInteraction(address a, address b) external onlyRecorder {
        require(a != address(0) && b != address(0), "Affinity: zero address");
        require(a != b, "Affinity: self interaction");

        _ensureKnown(a);
        _ensureKnown(b);

        bytes32 k = pairKey(a, b);
        pairInteractions[k] += 1;
        totalInteractions[a] += 1;
        totalInteractions[b] += 1;

        emit InteractionRecorded(a, b, pairInteractions[k]);

        uint256 aff = affinity(a, b);
        if (aff >= thetaA) {
            emit EdgeFormed(a, b, aff);
            _union(a, b);
        }
    }

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function _ensureKnown(address a) internal {
        if (!isKnown[a]) {
            isKnown[a] = true;
            _parent[a] = a;
        }
    }

    function _find(address a) internal returns (address) {
        address r = a;
        while (_parent[r] != r) {
            r = _parent[r];
        }
         
        address cur = a;
        while (_parent[cur] != r) {
            address next = _parent[cur];
            _parent[cur] = r;
            cur = next;
        }
        return r;
    }

    function findView(address a) public view returns (address) {
        if (!isKnown[a]) return address(0);
        address r = a;
        while (_parent[r] != r) {
            r = _parent[r];
        }
        return r;
    }

    function _union(address a, address b) internal {
        address ra = _find(a);
        address rb = _find(b);
        if (ra == rb) {
            _ensureGroup(ra);
            return;
        }

        address winner = ra < rb ? ra : rb;
        address loser = ra < rb ? rb : ra;

        uint256 gWinner = _ensureGroup(winner);
        uint256 gLoser = _groupIdOfRoot[loser];

        _parent[loser] = winner;

        if (gLoser != 0) {
            groupMemberCount[gWinner] += groupMemberCount[gLoser];
            groupMemberCount[gLoser] = 0;
            _groupIdOfRoot[loser] = 0;
            emit GroupsMerged(gWinner, gLoser);
        } else {
            groupMemberCount[gWinner] += 1;
        }
    }

    function _ensureGroup(address root) internal returns (uint256) {
        uint256 g = _groupIdOfRoot[root];
        if (g == 0) {
            groupCount += 1;
            g = groupCount;
            _groupIdOfRoot[root] = g;
            groupMemberCount[g] = 1;
            emit GroupCreated(g, root);
        }
        return g;
    }

    /** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
    function groupOf(address a) public view returns (uint256) {
        address r = findView(a);
        if (r == address(0)) return 0;
        return _groupIdOfRoot[r];
    }

    function inSameGroup(address a, address b) external view returns (bool) {
        uint256 ga = groupOf(a);
        return ga != 0 && ga == groupOf(b);
    }

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function setGroupPrimaryShard(uint256 groupId, uint256 shardId) external onlyAdmin {
        require(groupId != 0 && groupId <= groupCount, "Affinity: bad group");
        groupPrimaryShard[groupId] = shardId;
        groupHasShard[groupId] = true;
        emit GroupShardAssigned(groupId, shardId);
    }

    /** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
    function rebalanceGroups(uint256 epoch, uint256 shardCount, uint256[] calldata groupIds)
        external
        onlyAdmin
    {
        require(shardCount > 0, "Affinity: no shards");
        if (epoch != lastRebalanceEpoch) {
            delete _pendingBins;
            for (uint256 i = 0; i < shardCount; i++) {
                _pendingBins.push(0);
            }
            lastRebalanceEpoch = epoch;
        }
        require(_pendingBins.length == shardCount, "Affinity: shard count changed");

        uint256 prevVolume = type(uint256).max;
        for (uint256 i = 0; i < groupIds.length; i++) {
            uint256 g = groupIds[i];
            require(g != 0 && g <= groupCount, "Affinity: bad group");
            uint256 v = groupVolume[g];
            require(v <= prevVolume, "Affinity: not sorted desc");
            prevVolume = v;

            uint256 lightest = 0;
            for (uint256 j = 1; j < shardCount; j++) {
                if (_pendingBins[j] < _pendingBins[lightest]) {
                    lightest = j;
                }
            }
            _pendingBins[lightest] += v;
            groupPrimaryShard[g] = lightest;
            groupHasShard[g] = true;
            emit GroupShardAssigned(g, lightest);
        }
    }

    uint256[] private _pendingBins;

    function recordGroupVolume(uint256 groupId, uint256 volume) external onlyRecorder {
        require(groupId != 0 && groupId <= groupCount, "Affinity: bad group");
        groupVolume[groupId] = (groupVolume[groupId] + volume) / 2;
    }

    function primaryShardOf(address a) external view returns (bool hasShard, uint256 shardId) {
        uint256 g = groupOf(a);
        if (g == 0 || !groupHasShard[g]) return (false, 0);
        return (true, groupPrimaryShard[g]);
    }
}
