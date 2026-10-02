#!/usr/bin/env python3
"""Compare CSV values against preserved results; metadata timestamps/paths are excluded."""
import argparse
import csv
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def compare(actual, reference):
    with actual.open(encoding="utf-8-sig", newline="") as f:
        a = list(csv.reader(f))
    with reference.open(encoding="utf-8-sig", newline="") as f:
        b = list(csv.reader(f))
    if len(a) != len(b):
        return f"row count {len(a)} != {len(b)}"
    for i, (ra, rb) in enumerate(zip(a, b), 1):
        if len(ra) != len(rb):
            return f"row {i}: column count differs"
        for j, (va, vb) in enumerate(zip(ra, rb), 1):
            if va == vb:
                continue
            try:
                x, y = float(va), float(vb)
                if (math.isnan(x) and math.isnan(y)) or math.isclose(x, y, rel_tol=1e-9, abs_tol=1e-9):
                    continue
            except ValueError:
                pass
            return f"row {i}, column {j}: {va!r} != {vb!r}"
    return None

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--actual", type=Path, default=ROOT / "outputs/paper")
    parser.add_argument("--reference", type=Path, default=ROOT / "results/reference/paper")
    parser.add_argument("--files", nargs="+", default=["metrics.csv", "privacy.csv", "metrics_repeats.csv", "onchain_costs.csv"])
    args = parser.parse_args()
    failures = 0
    for name in args.files:
        a, b = args.actual / name, args.reference / name
        error = compare(a, b) if a.is_file() and b.is_file() else "missing CSV file"
        print(("FAIL" if error else "PASS"), name, error or "")
        failures += error is not None
    return int(failures > 0)

if __name__ == "__main__":
    raise SystemExit(main())
