// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface IAffinityRecorder {
    function recordInteraction(address a, address b) external;
}

/**
 * @title DigitalAssetRegistry
 * @dev Asset ownership, metadata-root commitments and transfer provenance.
 * Merkle paths verify disclosed leaf membership, not zero-knowledge predicates.
 * Transfers record interactions through AffinityManager. The registry does not enforce coordinator locks.
 */
contract DigitalAssetRegistry {

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    enum OpType { Create, Transfer, MetadataUpdate }

    struct Asset {
        address owner;
        uint256 shardId;
        bytes32 metadataRoot;      
        bytes32 encryptedRef;      
        uint256 createdAt;
        bool exists;
    }

    struct Record {
        OpType op;
        address from;
        address to;
        bytes32 metadataRoot;
        uint256 blockNumber;
        uint256 shardId;
    }

    address public admin;
    IAffinityRecorder public affinity;

    mapping(uint256 => Asset) public assets;
    mapping(uint256 => Record[]) private _provenance;
    uint256 public assetCount;

    mapping(bytes32 => bool) public relationshipCommitments;

    event AssetCreated(uint256 indexed assetId, address indexed owner, uint256 shardId);
    event AssetTransferred(uint256 indexed assetId, address indexed from, address indexed to);
    event MetadataUpdated(uint256 indexed assetId, bytes32 metadataRoot);
    event AttributeDisclosed(uint256 indexed assetId, bytes32 leaf);
    event RelationshipCommitted(bytes32 indexed commitment);

    modifier onlyOwner(uint256 assetId) {
        require(assets[assetId].exists, "Asset: not found");
        require(assets[assetId].owner == msg.sender, "Asset: not owner");
        _;
    }

    constructor(address _affinity) {
        admin = msg.sender;
        affinity = IAffinityRecorder(_affinity);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function createAsset(bytes32 metadataRoot, bytes32 encryptedRef, uint256 shardId)
        external
        returns (uint256 assetId)
    {
        assetId = ++assetCount;
        assets[assetId] = Asset({
            owner: msg.sender,
            shardId: shardId,
            metadataRoot: metadataRoot,
            encryptedRef: encryptedRef,
            createdAt: block.number,
            exists: true
        });
        _provenance[assetId].push(Record({
            op: OpType.Create,
            from: address(0),
            to: msg.sender,
            metadataRoot: metadataRoot,
            blockNumber: block.number,
            shardId: shardId
        }));
        emit AssetCreated(assetId, msg.sender, shardId);
    }

    function transferAsset(uint256 assetId, address to, uint256 newShardId)
        external
        onlyOwner(assetId)
    {
        require(to != address(0) && to != msg.sender, "Asset: bad recipient");
        Asset storage a = assets[assetId];
        address from = a.owner;

        a.owner = to;
        a.shardId = newShardId;

        _provenance[assetId].push(Record({
            op: OpType.Transfer,
            from: from,
            to: to,
            metadataRoot: a.metadataRoot,
            blockNumber: block.number,
            shardId: newShardId
        }));

        if (address(affinity) != address(0)) {
             
            try affinity.recordInteraction(from, to) {} catch {}
        }

        emit AssetTransferred(assetId, from, to);
    }

    function updateMetadata(uint256 assetId, bytes32 newRoot, bytes32 newEncryptedRef)
        external
        onlyOwner(assetId)
    {
        Asset storage a = assets[assetId];
        a.metadataRoot = newRoot;
        a.encryptedRef = newEncryptedRef;
        _provenance[assetId].push(Record({
            op: OpType.MetadataUpdate,
            from: msg.sender,
            to: msg.sender,
            metadataRoot: newRoot,
            blockNumber: block.number,
            shardId: a.shardId
        }));
        emit MetadataUpdated(assetId, newRoot);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function provenanceLength(uint256 assetId) external view returns (uint256) {
        return _provenance[assetId].length;
    }

    function provenanceAt(uint256 assetId, uint256 index)
        external
        view
        returns (OpType op, address from, address to, bytes32 root, uint256 blockNumber, uint256 shardId)
    {
        Record storage r = _provenance[assetId][index];
        return (r.op, r.from, r.to, r.metadataRoot, r.blockNumber, r.shardId);
    }

    function provenanceDigest(uint256 assetId) external view returns (bytes32 d) {
        Record[] storage rs = _provenance[assetId];
        for (uint256 i = 0; i < rs.length; i++) {
            d = keccak256(abi.encodePacked(
                d, uint8(rs[i].op), rs[i].from, rs[i].to,
                rs[i].metadataRoot, rs[i].blockNumber, rs[i].shardId
            ));
        }
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    function verifyAttribute(uint256 assetId, bytes32 leaf, bytes32[] calldata proof)
        public
        view
        returns (bool)
    {
        require(assets[assetId].exists, "Asset: not found");
        bytes32 computed = leaf;
        for (uint256 i = 0; i < proof.length; i++) {
            bytes32 p = proof[i];
            computed = computed <= p
                ? keccak256(abi.encodePacked(computed, p))
                : keccak256(abi.encodePacked(p, computed));
        }
        return computed == assets[assetId].metadataRoot;
    }

    function discloseAttribute(uint256 assetId, bytes32 leaf, bytes32[] calldata proof) external {
        require(verifyAttribute(assetId, leaf, proof), "Asset: invalid proof");
        emit AttributeDisclosed(assetId, leaf);
    }

    // ------------------------------------------------------------------
     
    // ------------------------------------------------------------------

    /// @param commitment keccak256(abi.encodePacked(oi, oj, salt))
    function commitRelationship(bytes32 commitment) external {
        require(commitment != bytes32(0), "Asset: empty commitment");
        relationshipCommitments[commitment] = true;
        emit RelationshipCommitted(commitment);
    }

    function verifyRelationship(address oi, address oj, bytes32 salt) external view returns (bool) {
        return relationshipCommitments[keccak256(abi.encodePacked(oi, oj, salt))];
    }

    // ------------------------------------------------------------------

    function setAffinity(address a) external {
        require(msg.sender == admin, "Asset: not admin");
        affinity = IAffinityRecorder(a);
    }
}
