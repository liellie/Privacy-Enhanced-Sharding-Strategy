# Components and measurement scope

## Execution paths

| Component | Responsibility |
|---|---|
| Python replay | Offline address placement, resource modeling and inference evaluation |
| Solidity contracts | Epoch state, resource aggregation, selection queries and coordination state transitions |
| Java services | Optional service integration, simulation receipts and experimental transaction submission |
| Chain drivers | Synthetic resource inputs, committee calls and collection of coordination-cost measurements |

The placement evaluation uses the supplied transaction trace. The cost evaluation uses one four-node QBFT chain with logical shards. Its metrics describe coordination calls and successful operation attempts; physical shard scaling, asset migration and complete asset-state execution are outside that measurement scope.

## Placement configuration

The paper profile uses full-trace threshold-graph components, Holt prediction, load sensitivity 0.01 and within-epoch feedback w=4.5. Enhanced's scheduling-load statistic uses prediction-fused loads and adaptive resource weights; baselines use raw loads and equal resource weights. These definitions apply when interpreting the archived comparisons.

The replay packs affinity groups at epoch boundaries and charges affinity quota during grouped-address decisions. The contract provides packing and quota-settlement entry points for explicit orchestration; the supplied cost driver invokes its measurement phases without calling those entry points. Its view-only selectors read cached epoch state without maintaining assignment counters. Contract grouping uses incremental union, preserving a merged group when its affinity later declines.

Load publication in the replay uses HMAC-SHA256 and floating-point arithmetic. The contract uses Keccak and a fixed-point inverse transform. The replay projects objective weights onto the ordinary simplex; the contract's configured component floor is 0.2. Both fallback selectors use a deterministic 0/1 locality cost. The chain deployment uses affinity threshold 0.5, while offline grouping uses 0.15.

## Coordination and service interfaces

- Oracle readings are aggregated by median. The chain driver supplies synthetic forecasts. Java submits zero prediction, reports each node's own shard for the current chain epoch, and schedules subsequent phases for the next epoch. Its database IDs require application mapping to zero-based chain shard IDs. A database epoch increment can persist when a later chain phase fails.
- Java's standard submission and file-loading endpoints store simulated success receipts. The experimental submission endpoint sends contract transactions. Routing error paths can select shard 0 while retaining an ONCHAIN label, so consumers should check receipt and error provenance when analyzing service outputs.
- CrossShardCoordinator commits coordination status and releases object locks. Binding all votes to a common read/write-set digest, verifying asset versions, applying asset mutations and enforcing those locks in DigitalAssetRegistry remain outside that state transition.
- Contract phase sequencing relies on caller orchestration. The entry points do not uniformly enforce epoch order or idempotence. Registration timing and beacon publication do not alone establish an immutable committee snapshot before randomness is known.
- Hash sortition can produce empty committees. The operation harness chooses serviceable shard pairs. The shared oracle reporter list and the hash-selected cross-shard committees serve distinct roles.

## Privacy interpretation

Raw oracle readings and the beacon are public, allowing full nodes to recompute deterministic perturbations. The inference evaluation models an observer of released summaries and placements. The fixed-state load bound of 0.0025 concerns the stated resource model; the implementation uses sensitivity 0.01.

Publication quotas are operational accounting. Quota recovery does not subtract from cumulative mathematical privacy loss. The observed attack statistics and group-repacking ablations describe empirical leakage under the specified estimators, rather than end-to-end differential privacy of the observable protocol. The archived replay did not exhaust its load-release quota.

Merkle disclosure verifies that a revealed attribute belongs to the committed tree. It is a membership proof, not a zero-knowledge predicate proof.
