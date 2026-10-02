# Reproducing the research results

## Environment and data

The core runner requires Python and `pycryptodome==3.23.0` only. Optional legacy plotting requires `experiments/requirements-plot.txt`. ARIMA is an optional legacy choice requiring statsmodels; the public paper profiles select Holt and do not need it.

Workload SHA-256: `65417b9f1efc977296157dd4c5af9307960a022b80153ed89b06a8fc43862e1d`.

The trace contains 47,482 records, 30,085 addresses and 2,176 blocks, split into 22 epochs of 100 blocks. There are 32,394 **unordered** non-self interacting pairs; ordered pairs number 33,361. The block interval has 2,176 positions, of which 2,148 are represented in the file.

## Fixed paper profile

| Parameter | Value |
|---|---|
| shards / blocks per replay epoch | 4 / 100 |
| seed / repeated seeds | 20260801 / 20260801 through 20260805 |
| affinity threshold | 0.15 |
| load / affinity epsilon bases | 0.2 / 0.8 |
| implementation load / affinity sensitivity | 0.01 / 0.5 |
| privacy budget maximum / rho / omega / gamma | 100 / 1 / 1 / 0.1 |
| predictor / alpha base / kappa | Holt / 0.3 / 1 |
| resource weight floor | 0.05 |
| controller eta / initial lambda | 0.1 / equal thirds |
| base tolerance / initial beta | 0.2 / 0.5 |
| intra-epoch feedback w | 4.5 |
| relationship evaluation pairs | 20,000, balanced positive/negative |
| spike epoch / factor | 12 / 3.0, with the source's two-epoch ramp |
| repeated frontier grid | 1.5, 1.8, 2.0, 2.4, 3.0, 3.6, 4.5, 6.0 |

These values specify the evaluated configuration. Resource formulas and capacities are in `experiments/src/strategies.py`.

## Commands

```bash
python scripts/reproduce.py --profile paper --only base,repeats,onchain
python scripts/compare_results.py

# All families; longer than the headline check.
python scripts/reproduce.py --profile paper

# Additional historical frontier/adaptability runs.
python scripts/reproduce.py --profile frontier
python scripts/compare_results.py --actual outputs/frontier --reference results/reference/frontier --files frontier.csv frontier_baselines.csv frontier_repeats.csv
python scripts/reproduce.py --profile adaptability
python scripts/compare_results.py --actual outputs/adaptability --reference results/reference/adaptability --files adaptability.csv adaptability_summary.csv
```

The full `paper` profile includes base, epsilon sweep, temporal attack, background knowledge, adaptability, frontier, packing, ablation, Byzantine simulation, composite-weight sweep, repeated seeds and archived on-chain aggregation. `--only` accepts comma-separated family names. See `experiments/replay_experiments.py --help` for the lower-level interface.

CSV comparison tolerates `1e-9` absolute/relative floating-point differences; it does not compare generated manifests, local output paths or environment strings. The smoke profile checks execution on 1,000 records; the paper profile covers the complete workload.

## Measurement conventions preserved from the source

1. Affinity groups are constructed from the **entire trace before replay**, not incrementally from a held-out training prefix. Treat this as an offline placement experiment. The graph baseline has its own observation/repartition schedule.
2. Enhanced `s.load` contains the prediction-fused load; baseline `s.load` contains raw weighted load. The reported standard deviation follows those values. Use these per-strategy definitions when interpreting the scheduling-load statistic.
3. Per-shard shares are normalized to sum to 100, then population standard deviation is taken. Epoch 0 is excluded from the aggregate load and per-resource averages. Cross-shard ratio is the total cross count divided by **all** replayed transactions, including epoch 0.
4. The load attack uses the published load as an estimate and scores matching equal-width bins defined from that epoch's true minimum/maximum. It is a particular estimator, not a bound on all adversaries and not an equal-frequency decile classifier.
5. The replay uses HMAC-SHA256 floating-point noise for load releases and the Keccak fixed-point helper for affinity noise. A MathProbe comparison does not test the main HMAC path.
6. The chain's shared resource reporting committee and the validator sortition committees are different mechanisms. Archived operation sampling excludes unserviceable shard pairs and retries some maintenance failures. Saved successful-operation latency and abort statistics must be interpreted with that selection process.

## Provenance

`results/reference/*/provenance.json` records experiment arguments, recorded environment and output hashes. `package-manifest.json` identifies this software snapshot. The paper, frontier and adaptability directories are separate configurations; w=2.4 in the frontier profile is intentional, while the paper profile uses w=4.5.

Historical chain addresses and transaction hashes are evidence identifiers. This release does not expose an RPC for independently fetching those old private-chain transactions. New microbenchmark measurements need fresh infrastructure, keys and receipts.

The three archived chain inputs use the shared display label `reference-study`. Measurement values, transaction identifiers, chain ID and timestamps are retained.
