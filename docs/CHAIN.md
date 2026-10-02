# Optional chain microbenchmark

The offline replay and archived-cost aggregation do not require a chain. The following tools create a new local four-node QBFT test network. They do not recover the old private-chain receipts, and a new run is not expected to reproduce identical latency or gas under a different build/environment.

Prerequisites: Docker with Linux containers, Bash (Git Bash on Windows), Python 3, Node.js with npm. The compiler script uses the pinned `ethereum/solc:0.8.19` image with optimizer runs=200. Supply an explicit `BESU_IMAGE` tag or digest; the original experiment did not record a verified version in its manifest. Record your choice with any new measurements.

```bash
npm --prefix deploy ci
# Set BESU_IMAGE to the exact image tag or digest selected for your experiment.
export BESU_IMAGE=hyperledger/besu:<your-pinned-tag>
bash chain/scripts/01-init-network.sh
bash chain/scripts/02-up.sh
bash deploy/compile.sh
node deploy/scripts/deploy.js
node deploy/scripts/epoch-driver.js --epochs 10 --byzantine --run-id local-study --out-dir outputs/onchain
```

Replace the image placeholder before executing. The initializer generates fresh validator and coordinator identities and refuses to overwrite an existing network. It writes `chain/.env` and generated node key files locally. Deployment may write signing identities into `deployed.json`; these files are deliberately ignored and must stay private. RPC ports are bound to localhost in the Compose file. Containers retain the internal `172.28.0.0/16` test network, which is a template topology rather than an exported service address.

## Measuring operations and oracle costs

The driver uses synthetic resource values with jitter, four reporters per shard, and a fixed counter input. It measures protocol phases, not the end-to-end replay scheduler. The deployment affinity threshold is 0.5, distinct from the offline 0.15. Group repacking and affinity-budget settlement are not scheduled by this driver.

After creating and publishing shard state:

```bash
node deploy/scripts/register-shard-addresses.js
node deploy/scripts/generate-ops.js --preflight
# Inject and verify the requested network delay before collecting timed measurements.
bash chain/scripts/03-netem.sh delay 50 10
node deploy/scripts/generate-ops.js --count 120 --cross-ratio 0.5 --run-id local-study --out-dir outputs/onchain
```

The impairment helper uses Pumba. Set `PUMBA_IMAGE` to a pinned image and document the tc helper version for new studies. Merely passing `--netem-delay-ms` to a measurement script does not inject delay; the metadata is a declaration, not an independent verification.

The driver command above collects ten rounds in one invocation and also writes the Byzantine input. Do not run it ten times in the same output directory: a new invocation replaces its JSON output. Use a separate output directory and run ID for each independent study. All three JSON inputs must describe the same study. Archived JSON inputs can be aggregated using the offline paper profile without starting services.

The operation harness uses prepared test-address groups, selects shards with available committees and retries certain maintenance/timeout errors. The logged successful-operation latency excludes those discarded attempts. Its 50/50 intra/cross mix is a sampling choice, not an estimate of the workload's cross-shard ratio. The coordinator is a lock/coordination prototype; see `IMPLEMENTATION_SCOPE.md` before interpreting atomicity or security properties.

Stop only this network using `docker compose -f chain/docker-compose.yml down`. Generated keys and chain data are not deleted by that command. Validation coverage is recorded in [VALIDATION.md](VALIDATION.md).
