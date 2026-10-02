# ShardChain — Privacy-Enhanced Sharding

Companion research prototype for **Privacy-Enhanced Sharding Strategy in Digital Asset Sharing Based on Organizational Affinity and Load Awareness**.

This release contains the trace replay used to study placement quality, archived experiment results, Solidity protocol prototypes, and an optional Java control plane. The primary reproducible experiment runs locally without Ethereum, Docker, Java, MySQL, Nacos, or deployment credentials.

**Scope:** placement and inference metrics come from offline replay. Latency and gas metrics come from a separate logical-sharding microbenchmark on one QBFT chain. The Java services are an integration prototype, not the source of the paper's placement results. See [component roles and interfaces](docs/IMPLEMENTATION_SCOPE.md) for the execution paths.

[中文说明](README.zh-CN.md) · [Reproduction guide](docs/REPRODUCIBILITY.md) · [Results map](docs/RESULTS_MAP.md) · [Release changes](docs/RELEASE_NOTES.md)

## Quick start

Use Python 3.12 and run from the repository root. See [validation](docs/VALIDATION.md) for the tested environment.

```bash
python -m venv .venv
# Linux/macOS: source .venv/bin/activate
# PowerShell: .\.venv\Scripts\Activate.ps1
python -m pip install -r experiments/requirements.txt

# Small functional check using 1,000 records.
python scripts/reproduce.py --profile smoke

# Full trace, four strategies, five-seed headline statistics, and archived chain-cost aggregation.
python scripts/reproduce.py --profile paper --only base,repeats,onchain
python scripts/compare_results.py
```

For all experiment families, including parameter sweeps and ablations:

```bash
python scripts/reproduce.py --profile paper
```

No `.git` directory or initial commit is required. Fresh outputs go to `outputs/`; archived results remain in `results/reference/`. The default profile fixes the published operating point at `w=4.5`, `epsilon1=0.2`, `epsilon2=0.8`, four shards and the Holt predictor.

## Included components

| Path | Role |
|---|---|
| `experiments/src/` | Resource model, four placement strategies, privacy primitives, attack estimators |
| `experiments/data/transactions.txt` | Mainnet-derived workload extract, 47,482 records |
| `scripts/reproduce.py` | Portable experiment profiles |
| `scripts/compare_results.py` | Numerical CSV comparison against archived results |
| `results/reference/` | Archived CSVs and experiment configurations |
| `results/onchain-input/` | Historical microbenchmark measurements used for aggregation |
| `contracts/` | Solidity beacon, oracle, scheduler, controller, registry and coordinator prototypes |
| `chain/`, `deploy/` | Optional local QBFT setup and measurement tools; generate your own keys |
| `java-control-plane/` | Optional Java 21/Spring services; source and configuration templates only |
| `package-manifest.json` | Checksums of the release snapshot |

## Archived headline results

These are the saved five-seed results, not measurements taken from a Java deployment.

| Metric | Enhanced |
|---|---:|
| Cross-shard operation ratio | 48.880% ± 0.308% |
| Load standard deviation | 1.526 ± 0.063 percentage points |
| Load inference accuracy | 32.955% ± 3.503% |
| Relationship inference accuracy | 53.424% ± 0.190% |
| Composite score | 64.570 ± 0.224 |

Relationship accuracy describes the evaluated co-location classifier. The full four-strategy comparison is in `results/reference/paper/privacy.csv`.

The Graph baseline cross-shard ratio is 69.757%; the relative reduction is 29.928%. Sources: `results/reference/paper/metrics_repeats.csv`. The metric conventions, full-trace grouping, warm-up exclusion, and fused-load evaluation are specified in the reproduction guide.

## Optional system prototypes

[Chain instructions](docs/CHAIN.md) describe fresh-key generation, compilation and independent cost measurements. [Java instructions](java-control-plane/README.md) describe source structure and manual configuration. Neither is required for the offline reproduction commands above. Archived chain costs can be recomputed without connecting to a live RPC; remeasuring their wall-clock values requires a new deployment and will vary with the environment.

## Integrity and publication hygiene

```bash
python scripts/verify_package.py
python scripts/check_publication.py
```

The first command checks this release snapshot; intentional edits invalidate the snapshot hashes. The second is a heuristic credential/runtime-file check, not a guarantee that every possible secret is detectable. No original Git history, deployed account file, private keys, credentials, logs or build caches are included. Public transaction addresses/hashes remain in the research trace and measurement data.

The code uses the [MIT license](LICENSE), inherited from the research repository. The [data note](experiments/data/README.md) describes the workload separately. Cite the associated paper when using the research method or results; [CITATION.cff](CITATION.cff) identifies this software artifact.
