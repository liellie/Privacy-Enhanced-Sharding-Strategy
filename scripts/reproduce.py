#!/usr/bin/env python3
"""Portable entry point for the preserved paper experiments (no live chain required)."""
from pathlib import Path
import argparse
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=["smoke", "paper", "frontier", "adaptability"], default="paper")
    parser.add_argument("--only", help="Override the selected experiment names, comma separated")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    out = args.out or ROOT / "outputs" / args.profile
    command = [sys.executable, str(ROOT / "experiments/replay_experiments.py"),
               "--data", str(ROOT / "experiments/data/transactions.txt"), "--out", str(out),
               "--no-chain-check", "--shards", "4", "--blocks-per-epoch", "100",
               "--theta", "0.15", "--epsilon", "0.2", "--seed", "20260801",
               "--predictor", "holt", "--intra-epoch-feedback", "4.5",
               "--repeats", "5", "--eval-pairs", "20000",
               "--frontier-repeat-ws", "1.5,1.8,2.0,2.4,3.0,3.6,4.5,6.0",
               "--packing-ws", "1.5,2.4,3.0,4.5",
               "--onchain-metrics", str(ROOT / "results/onchain-input")]
    selected = args.only
    if args.profile == "smoke":
        command += ["--limit", "1000", "--eval-pairs", "400", "--repeats", "1"]
        selected = selected or "base"
    elif args.profile == "frontier":
        command += ["--intra-epoch-feedback", "2.4", "--packing-ws", "1.5,1.8,2.0,2.4"]
        selected = selected or "frontier"
    elif args.profile == "adaptability":
        selected = selected or "adapt"
    command += ["--only", selected] if selected else ["--all"]
    print("Profile:", args.profile, "| output:", out, flush=True)
    return subprocess.call(command, cwd=ROOT)

if __name__ == "__main__":
    raise SystemExit(main())
