# Reproduction checks

The recorded checks cover offline replay and aggregation of archived QBFT measurements, using Python 3.12 and pycryptodome 3.23.0.

| Check | Result |
|---|---|
| Full paper profile over 47,482 records | Completed |
| Comparison of all 19 result CSVs | Matched at 1e-9 absolute/relative tolerance |
| Five-seed summary CSV | Byte-identical to the reference |
| 1,000-record smoke profile | Completed |
| Python, JavaScript and shell syntax | Passed |
| JSON parsing and local documentation links | Passed |
| Public file inventory and SHA-256 hashes | Verified |

## Run the checks

```bash
python scripts/reproduce.py --profile paper
python scripts/compare_results.py
python scripts/check_publication.py
python scripts/verify_package.py
```

`compare_results.py` checks the four headline CSVs by default. Use `--files` to select additional tables. [RESULTS_MAP.md](RESULTS_MAP.md) lists the experiment families and outputs. [IMPLEMENTATION_SCOPE.md](IMPLEMENTATION_SCOPE.md) defines the components and measurement boundaries.
