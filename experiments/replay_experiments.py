#!/usr/bin/env python3
"""Offline placement replay and archived QBFT measurement aggregation.
Use scripts/reproduce.py for the fixed paper, smoke, frontier and adaptability profiles. Numeric outputs are deterministic for the same input and parameters; manifests record the execution environment."""

from __future__ import annotations

import argparse
import csv
import datetime
import hashlib
import json
import math
import platform
import random
import statistics
import subprocess
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from run_experiment import build_strategies, run_one
from src.attacks import load_inference_accuracy, relationship_inference_accuracy
from src.dataset import Dataset
from src.privacy import PrivacyBudget, WAD
from src.strategies import EnhancedStrategy, ResourceModel, DIMS

def write_csv(path: Path, rows: list[dict], columns: list[str]) -> None:
    """Write CSV rows with an explicit header, including when the row set is empty."""
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)
    if path.stat().st_size == 0:
        raise SystemExit(f"输出文件为空: {path}")

def rpc(url: str, method: str, params):
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode()
    request = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.loads(response.read().decode())
    if "error" in result:
        raise RuntimeError(result["error"])
    return result["result"]

def chain_context(rpc_url: str, deployed_path: Path) -> dict:
    """Read static chain identifiers and contract addresses from deployment metadata.
Exclude changing chain state such as the latest block height from this snapshot."""
    deployed = json.loads(deployed_path.read_text(encoding="utf-8"))
    return {
        "rpc": rpc_url,
        "chain_id": deployed.get("chainId"),
        "contracts": deployed.get("contracts", {}),
        "interpretation": (
            "The deployed QBFT network is one logical-shard test chain, "
            "not independent shard chains."
        ),
    }

def parse_csv_range(spec: str) -> list[float]:
    """Parse an inclusive start:stop:step range using rounded values to avoid accumulated floating-point drift."""
    start, stop, step = (float(x) for x in spec.split(":"))
    n = int(round((stop - start) / step))
    return [round(start + i * step, 6) for i in range(n + 1)]

def parse_csv_list(spec: str) -> list[float]:
    return [float(x.strip()) for x in spec.split(",") if x.strip()]

def summarize_row(name: str, result: dict, strategy, load_weight: float = 0.4) -> dict:
    """Aggregate epoch metrics; load_weight selects the balance contribution to the composite score."""
    stable = result["per_epoch"][1:] or result["per_epoch"]
    load_cvs, load_pps = [], []
    for e in stable:
        loads = e["loads"]
        mean = statistics.fmean(loads)
        tot = sum(loads)
        if mean > 0:
            load_cvs.append(statistics.pstdev(loads) / mean)
        if tot > 0:
            shares = [l / tot * 100 for l in loads]
            load_pps.append(statistics.pstdev(shares))
    load_std = statistics.fmean(load_cvs) if load_cvs else 0.0
    load_pp = statistics.fmean(load_pps) if load_pps else 0.0

    dim_std, dim_par = {}, {}
    for d in DIMS:
        stds, pars = [], []
        for e in stable:
            vals = [n[d] for n in e["norm"]]
            m = statistics.fmean(vals)
            if m > 0:
                stds.append(statistics.pstdev(vals) / m)
                pars.append(max(vals) / m)
        dim_std[d] = statistics.fmean(stds) if stds else 0.0
        dim_par[d] = statistics.fmean(pars) if pars else 0.0

    locality_score = max(0.0, 100.0 - result["cross_ratio"] * 100.0)
    balance_score = max(0.0, 100.0 - 10.0 * load_pp)
    composite = load_weight * balance_score + (1 - load_weight) * locality_score

    row = {
        "strategy": name,
        "total_tx": result["total_tx"],
        "cross_shard_ops": result["total_cross"],
        "cross_shard_ratio_pct": result["cross_ratio"] * 100,
        "load_cv_pct": load_std * 100,
        "load_std_pp": load_pp,
        "composite_score": composite,
    }
    for d in DIMS:
        row[f"std_{d}_pct"] = dim_std[d] * 100
        row[f"par_{d}"] = dim_par[d]
    if isinstance(strategy, EnhancedStrategy):
        row["budget_exhausted_events"] = strategy.budget.exhausted_events
        row["publish_events"] = strategy.budget.publish_events
        row["stale_publish_ratio_pct"] = (
            strategy.budget.stale_publish_events / strategy.budget.publish_events * 100
            if strategy.budget.publish_events else 0.0
        )
    return row

def load_infer_pct(result: dict) -> float:
    published = [e["published"] for e in result["per_epoch"]]
    loads = [e["loads"] for e in result["per_epoch"]]
    return load_inference_accuracy(published, loads) * 100

def relation_pct(result: dict, true_groups: dict, eval_pairs: list, seed: int,
                 known_fraction: float = 0.0) -> dict:
    return relationship_inference_accuracy(
        result["assignment_history"], true_groups,
        known_fraction=known_fraction,
        max_pairs=len(eval_pairs) if eval_pairs else 20000,
        seed=seed, eval_pairs=eval_pairs,
    )

def load_infer_chance_stats(published: list[list[float]], loads: list[list[float]],
                            runs: int, seed: int) -> tuple[float, float]:
    """Shuffle published loads within each epoch and summarize the resulting inference scores.
This permutation reference measures correspondence to shard labels; it is not a privacy guarantee."""
    rng = random.Random(seed)
    accs = []
    for _ in range(runs):
        shuffled: list[list[float]] = []
        for epoch_pub in published:
            p = list(epoch_pub)
            rng.shuffle(p)
            shuffled.append(p)
        accs.append(load_inference_accuracy(shuffled, loads) * 100)
    return (statistics.fmean(accs),
            statistics.stdev(accs) if len(accs) > 1 else 0.0)

def run_all_strategies(k, groups, seed, epochs, model, predictor="persistence",
                       intra_epoch_feedback=1.5, epsilon_base=0.2) -> dict[str, tuple]:
    results = {}
    for strategy in build_strategies(k, groups, seed, predictor=predictor,
                                     intra_epoch_feedback=intra_epoch_feedback,
                                     epsilon_base=epsilon_base):
        result = run_one(strategy, epochs, model, verbose=False)
        results[strategy.name] = (strategy, result)
    return results

def load_pipeline(args) -> tuple:
    """Load the trace, construct affinity groups and sample the shared evaluation pairs."""
    data_path = Path(args.data).resolve()
    if not data_path.exists():
        raise SystemExit(f"未找到交易数据: {data_path}")
    print(f"==> workload: {data_path}")
    dataset = Dataset.load(data_path, limit=args.limit, seed=args.seed)
    summary = dataset.summary()
    summary["power_law_exponent"] = dataset.power_law_exponent()
    groups = dataset.affinity_groups(args.theta)
    true_groups = dataset.true_affinity_groups(args.theta)
    summary["affinity"] = {"theta": args.theta, **dataset.group_stats(groups),
                           **dataset.activity_stats(groups)}
    epochs = dataset.split_epochs(args.blocks_per_epoch)
    eval_pairs, eval_meta = dataset.sample_eval_pairs(
        true_groups, args.eval_pairs, seed=args.seed, blocks_per_epoch=args.blocks_per_epoch
    )
    summary["source_file"] = "experiments/data/" + data_path.name
    summary["source_sha256"] = hashlib.sha256(data_path.read_bytes()).hexdigest()
    summary["eval_pairs"] = eval_meta
    return dataset, summary, groups, true_groups, epochs, eval_pairs

def exp_base(args, out: Path, ctx) -> list[str]:
    dataset, summary, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    out.joinpath("dataset_summary.json").write_text(
        json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")

    results = run_all_strategies(args.shards, groups, args.seed, epochs, model,
                                 predictor=args.predictor,
                                 intra_epoch_feedback=args.intra_epoch_feedback,
                                 epsilon_base=args.epsilon)
    metric_rows = [summarize_row(name, res, st) for name, (st, res) in results.items()]
    k = args.shards
    factor = round(math.sqrt(k / (k - 1)), 6) if k > 1 else 1.0
    for row in metric_rows:                      
        row["load_std_method"] = "population (pstdev)"
        row["sample_std_factor_sqrt_k_kminus1"] = factor
        row["intra_epoch_feedback"] = args.intra_epoch_feedback
        row["predictor"] = args.predictor
    write_csv(out / "metrics.csv", metric_rows,
              ["strategy", "total_tx", "cross_shard_ops", "cross_shard_ratio_pct",
               "load_cv_pct", "load_std_pp", "composite_score",
               *(f"std_{d}_pct" for d in DIMS), *(f"par_{d}" for d in DIMS),
               "budget_exhausted_events", "publish_events", "stale_publish_ratio_pct",
               "load_std_method", "sample_std_factor_sqrt_k_kminus1",
               "intra_epoch_feedback", "predictor"])

    perm_runs = args.permutation_runs or args.repeats
    privacy_rows = []
    for name, (st, res) in results.items():
        rel = relation_pct(res, true_groups, eval_pairs, args.seed)
        chance_mean, chance_std = load_infer_chance_stats(
            [e["published"] for e in res["per_epoch"]],
            [e["loads"] for e in res["per_epoch"]],
            perm_runs, args.seed)
        privacy_rows.append({
            "strategy": name,
            "load_inference_pct": load_infer_pct(res),
            "load_inference_chance_pct": chance_mean,
            "load_inference_chance_std_pct": chance_std,
            "relationship_inference_pct": rel["accuracy"] * 100,
            "relationship_precision_pct": rel["precision"] * 100,
            "relationship_recall_pct": rel["recall"] * 100,
            "pairs_evaluated": rel["pairs"],
            "positive_base_rate_pct": rel["positive_base_rate_pct"],
            "threshold": rel["threshold"],
        })
    write_csv(out / "privacy.csv", privacy_rows,
              ["strategy", "load_inference_pct", "load_inference_chance_pct",
               "load_inference_chance_std_pct", "relationship_inference_pct",
               "relationship_precision_pct", "relationship_recall_pct",
               "pairs_evaluated", "positive_base_rate_pct", "threshold"])

    write_csv(out / "resource_dims.csv", [
        {"strategy": row["strategy"],
         **{f"std_{dim}_pct": row[f"std_{dim}_pct"] for dim in DIMS},
         **{f"par_{dim}": row[f"par_{dim}"] for dim in DIMS}}
        for row in metric_rows
    ], ["strategy", *(f"std_{d}_pct" for d in DIMS), *(f"par_{d}" for d in DIMS)])

    epoch_rows_out = []
    for name, (_, res) in results.items():
        for e in res["per_epoch"]:
            loads = e["loads"]
            total = sum(loads)
            shares = [v / total * 100 for v in loads] if total else [0] * len(loads)
            epoch_rows_out.append({
                "strategy": name,
                "epoch": e["epoch"],
                "n_tx": e["n_tx"],
                "n_cross": e["n_cross"],
                "cross_shard_ratio_pct": round(e["cross_ratio"] * 100, 6),
                "load_std_pp": round(statistics.pstdev(shares), 6),
                "load_shares_pct": ";".join(f"{v:.6f}" for v in shares),
                "load_std_method": "population (pstdev)",
            })
    write_csv(out / "epoch_metrics.csv", epoch_rows_out,
              ["strategy", "epoch", "n_tx", "n_cross", "cross_shard_ratio_pct",
               "load_std_pp", "load_shares_pct", "load_std_method"])

    ablation = []
    for label, rebalance in (("Sticky mapping", False), ("Per-epoch rebalance", True)):
        strategy = EnhancedStrategy(args.shards, groups, seed=args.seed,
                                    rebalance_groups=rebalance,
                                    predictor=args.predictor,
                                    intra_epoch_feedback=args.intra_epoch_feedback,
                                    epsilon_base=args.epsilon)
        result = run_one(strategy, epochs, model, verbose=False)
        row = summarize_row("Enhanced", result, strategy)
        rel = relation_pct(result, true_groups, eval_pairs, args.seed)
        ablation.append({
            "variant": label,
            "cross_shard_ratio_pct": row["cross_shard_ratio_pct"],
            "load_std_pp": row["load_std_pp"],
            "relationship_inference_pct": rel["accuracy"] * 100,
        })
    write_csv(out / "ablation_rebalance.csv", ablation,
              ["variant", "cross_shard_ratio_pct", "load_std_pp", "relationship_inference_pct"])

    if args.no_chain_check:
        context = {"chain_check": "skipped"}
    else:
        context = chain_context(args.rpc, Path(args.deployed).resolve())
    out.joinpath("chain_context.json").write_text(
        json.dumps(context, indent=2, ensure_ascii=False), encoding="utf-8")
    return ["dataset_summary.json", "metrics.csv", "privacy.csv", "resource_dims.csv",
            "epoch_metrics.csv", "ablation_rebalance.csv", "chain_context.json"]

def exp_eps(args, out: Path, ctx, eps_values: list[float]) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    seeds = args.seeds if args.seeds else [args.seed + i for i in range(args.repeats)]
    metrics = ["cross_shard_ratio_pct", "load_std_pp", "load_cv_pct",
               "load_inference_pct", "relationship_inference_pct",
               "budget_exhausted_events", "publish_events", "stale_publish_ratio_pct"]
    rows = []
    for eps in eps_values:
        rec: dict[str, list[float]] = {m: [] for m in metrics}
        for s in seeds:
            st = EnhancedStrategy(args.shards, groups, seed=s,
                                  predictor=args.predictor,
                                  intra_epoch_feedback=args.intra_epoch_feedback,
                                  epsilon_base=args.epsilon)
            st.budget = PrivacyBudget(epsilon_base_wad=int(eps * WAD))
            res = run_one(st, epochs, model, verbose=False)
            row = summarize_row("Enhanced", res, st)
            rel = relation_pct(res, true_groups, eval_pairs, s)
            rec["cross_shard_ratio_pct"].append(row["cross_shard_ratio_pct"])
            rec["load_std_pp"].append(row["load_std_pp"])
            rec["load_cv_pct"].append(row["load_cv_pct"])
            rec["load_inference_pct"].append(load_infer_pct(res))
            rec["relationship_inference_pct"].append(rel["accuracy"] * 100)
            rec["budget_exhausted_events"].append(row["budget_exhausted_events"])
            rec["publish_events"].append(row["publish_events"])
            rec["stale_publish_ratio_pct"].append(row["stale_publish_ratio_pct"])
        out_row: dict = {"epsilon": eps}
        for m in metrics:
            vals = rec[m]
            out_row[f"{m}_mean"] = statistics.fmean(vals)
            out_row[f"{m}_std"] = statistics.stdev(vals) if len(vals) > 1 else 0.0
        out_row["n_runs"] = len(seeds)
        out_row["seeds"] = ";".join(str(s) for s in seeds)
        rows.append(out_row)
        print(f"    eps={eps:<5} 关系推断={out_row['relationship_inference_pct_mean']:.1f}%  "
              f"负载推断={out_row['load_inference_pct_mean']:.1f}%  "
              f"跨片={out_row['cross_shard_ratio_pct_mean']:.1f}%  "
              f"(n={len(seeds)})")
    columns = ["epsilon"]
    for m in metrics:
        columns += [f"{m}_mean", f"{m}_std"]
    columns += ["n_runs", "seeds"]
    write_csv(out / "epsilon_sweep.csv", rows, columns)
    return ["epsilon_sweep.csv"]

def exp_temporal(args, out: Path, ctx) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    
    epoch_end = []
    for ep in epochs:
        times = [datetime.datetime.fromisoformat(t.timestamp) for t in ep]
        epoch_end.append(max(times))
    windows = sorted({min(w, len(epochs)) for w in [1, 2, 4, 8, 16, 22] if min(w, len(epochs)) >= 1})
    rows = []
    for w in windows:
        elapsed = (epoch_end[w - 1] - epoch_end[0]).total_seconds() / 60.0
        for strategy in build_strategies(args.shards, groups, args.seed,
                                         predictor=args.predictor,
                                         intra_epoch_feedback=args.intra_epoch_feedback,
                                         epsilon_base=args.epsilon):
            result = run_one(strategy, epochs, model, verbose=False)
            hist = result["assignment_history"][:w]
            rel = relationship_inference_accuracy(hist, true_groups, seed=args.seed,
                                                  max_pairs=len(eval_pairs), eval_pairs=eval_pairs)
            pub = [e["published"] for e in result["per_epoch"][:w]]
            loads = [e["loads"] for e in result["per_epoch"][:w]]
            rows.append({
                "strategy": strategy.name,
                "window_epochs": w,
                "elapsed_minutes": round(elapsed, 2),
                "load_inference_pct": load_inference_accuracy(pub, loads) * 100,
                "relationship_inference_pct": rel["accuracy"] * 100,
            })
    write_csv(out / "temporal_attack.csv", rows,
              ["strategy", "window_epochs", "elapsed_minutes",
               "load_inference_pct", "relationship_inference_pct"])
    return ["temporal_attack.csv"]

def exp_bg(args, out: Path, ctx) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    rows = []
    for frac in [0.10, 0.20, 0.40, 0.60]:
        for strategy in build_strategies(args.shards, groups, args.seed,
                                         predictor=args.predictor,
                                         intra_epoch_feedback=args.intra_epoch_feedback,
                                         epsilon_base=args.epsilon):
            result = run_one(strategy, epochs, model, verbose=False)
            rel = relation_pct(result, true_groups, eval_pairs, args.seed,
                               known_fraction=frac)
            rows.append({
                "strategy": strategy.name,
                "known_fraction_pct": frac * 100,
                "pairs_evaluated": rel["pairs"],
                "relationship_inference_pct": rel["accuracy"] * 100,
                "precision_pct": rel["precision"] * 100,
                "recall_pct": rel["recall"] * 100,
            })
    write_csv(out / "background_knowledge.csv", rows,
              ["strategy", "known_fraction_pct", "pairs_evaluated",
               "relationship_inference_pct", "precision_pct", "recall_pct"])
    return ["background_knowledge.csv"]

def exp_adapt(args, out: Path, ctx) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    spike_model = ResourceModel(spike_epoch=args.spike_epoch, spike_factor=args.spike_factor)

    predictors = [args.predictor] if args.predictor == "persistence" \
        else [args.predictor, "persistence"]
    rows, summaries = [], []
    for predictor in predictors:
        for strategy in build_strategies(args.shards, groups, args.seed,
                                         predictor=predictor,
                                         intra_epoch_feedback=args.intra_epoch_feedback,
                                         epsilon_base=args.epsilon):
            result = run_one(strategy, epochs, spike_model, verbose=False)
            srows = []                                   
            for i, e in enumerate(result["per_epoch"]):
                loads = e["loads"]
                total = sum(loads)
                shares = [v / total * 100 for v in loads] if total else [0] * len(loads)
                true0 = strategy.raw_used_history[i][0]
                pred0 = strategy.pred_used_history[i][0]
                srows.append({
                    "strategy": strategy.name,
                    "predictor": predictor,
                    "epoch": e["epoch"],
                    "load_std_pp": round(statistics.pstdev(shares), 6),
                    "shard0_true_load": true0,
                    "shard0_predicted_load": round(pred0, 6),
                    "shard0_published_load": e["published"][0],
                })

            lo = max(0, args.spike_epoch - 7)
            hi = max(lo, args.spike_epoch - 3)          
            base_true = [strategy.raw_used_history[i][0] for i in range(lo, min(hi + 1, len(result["per_epoch"])))]
            base_std = [srows[i]["load_std_pp"] for i in range(lo, min(hi + 1, len(srows)))]
            if not base_true:
                summaries.append({"strategy": strategy.name, "predictor": predictor,
                                  "lead_epochs": "N/A", "recovery_epochs": "N/A",
                                  "peak_load_std_pp": 0.0, "peak_epoch": -1,
                                  "achieved_spike_factor": 0.0})
                rows.extend(srows)
                continue
            thr = statistics.fmean(base_true) + 2 * statistics.pstdev(base_true)
            pred_cross = None
            true_cross = None
            for i in range(len(result["per_epoch"])):
                pv = strategy.pred_used_history[i][0]
                tv = strategy.raw_used_history[i][0]
                if pred_cross is None and pv >= thr:
                    pred_cross = i
                if true_cross is None and tv >= thr:
                    true_cross = i
            
            lead = (true_cross - pred_cross) if (pred_cross is not None and true_cross is not None) else "N/A"
            base_std_mean = statistics.fmean(base_std) if base_std else 0.0
            target = 1.1 * base_std_mean
            peak_val = max(r["load_std_pp"] for r in srows)
            peak_ep = max(range(len(srows)), key=lambda i: srows[i]["load_std_pp"])
            
            first_ok = next((i for i in range(peak_ep, len(srows))
                             if srows[i]["load_std_pp"] <= target), None)
            recovery = (first_ok - peak_ep + 1) if first_ok is not None else "N/A"
            base_true_mean = statistics.fmean(base_true) if base_true else 0.0
            peak_true = max(strategy.raw_used_history[i][0] for i in range(len(result["per_epoch"])))
            achieved = (peak_true / base_true_mean) if base_true_mean > 0 else 0.0
            summaries.append({"strategy": strategy.name, "predictor": predictor,
                              "lead_epochs": lead, "recovery_epochs": recovery,
                              "peak_load_std_pp": peak_val, "peak_epoch": peak_ep,
                              "achieved_spike_factor": round(achieved, 6)})
            rows.extend(srows)
    write_csv(out / "adaptability.csv", rows,
              ["strategy", "predictor", "epoch", "load_std_pp", "shard0_true_load",
               "shard0_predicted_load", "shard0_published_load"])
    write_csv(out / "adaptability_summary.csv", summaries,
              ["strategy", "predictor", "lead_epochs", "recovery_epochs",
               "peak_load_std_pp", "peak_epoch", "achieved_spike_factor"])
    return ["adaptability.csv", "adaptability_summary.csv"]

def exp_frontier(args, out: Path, ctx, weights: list[float]) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    rows = []
    for w in weights:
        st = EnhancedStrategy(args.shards, groups, seed=args.seed,
                              predictor=args.predictor, intra_epoch_feedback=w,
                              epsilon_base=args.epsilon)
        res = run_one(st, epochs, model, verbose=False)
        row = summarize_row("Enhanced", res, st)
        rel = relation_pct(res, true_groups, eval_pairs, args.seed)
        rows.append({
            "w": w,
            "is_operating_point": bool(abs(w - args.intra_epoch_feedback) < 1e-9),
            "cross_shard_ratio_pct": row["cross_shard_ratio_pct"],
            "load_std_pp": row["load_std_pp"],
            "relationship_inference_pct": rel["accuracy"] * 100,
            "composite_score": row["composite_score"],
        })
    baselines = []
    for name, (st, res) in run_all_strategies(args.shards, groups, args.seed, epochs, model,
                                              predictor=args.predictor,
                                              intra_epoch_feedback=args.intra_epoch_feedback,
                                              epsilon_base=args.epsilon).items():
        row = summarize_row(name, res, st)
        rel = relation_pct(res, true_groups, eval_pairs, args.seed)
        baselines.append({
            "w": "",
            "strategy": name,
            "cross_shard_ratio_pct": row["cross_shard_ratio_pct"],
            "load_std_pp": row["load_std_pp"],
            "relationship_inference_pct": rel["accuracy"] * 100,
            "composite_score": row["composite_score"],
        })
    write_csv(out / "frontier.csv", rows,
              ["w", "is_operating_point", "cross_shard_ratio_pct", "load_std_pp",
               "relationship_inference_pct", "composite_score"])
    write_csv(out / "frontier_baselines.csv", baselines,
              ["w", "strategy", "cross_shard_ratio_pct", "load_std_pp",
               "relationship_inference_pct", "composite_score"])

    rep_seeds = args.seeds if args.seeds else [args.seed + i for i in range(args.repeats)]
    rep_rows = []
    rep_ws = sorted(set(parse_csv_list(args.frontier_repeat_ws))
                    | {round(args.intra_epoch_feedback, 6)})
    for w in rep_ws:
        lss, csr, cmp, rip = [], [], [], []
        for s in rep_seeds:
            st = EnhancedStrategy(args.shards, groups, seed=s,
                                  predictor=args.predictor, intra_epoch_feedback=w,
                                  epsilon_base=args.epsilon)
            res = run_one(st, epochs, model, verbose=False)
            row = summarize_row("Enhanced", res, st)
            rel = relation_pct(res, true_groups, eval_pairs, s)
            lss.append(row["load_std_pp"])
            csr.append(row["cross_shard_ratio_pct"])
            cmp.append(row["composite_score"])
            rip.append(rel["accuracy"] * 100)
        rep_rows.append({
            "w": w,
            "load_std_pp_mean": statistics.fmean(lss),
            "load_std_pp_std": statistics.stdev(lss) if len(lss) > 1 else 0.0,
            "cross_shard_ratio_pct_mean": statistics.fmean(csr),
            "cross_shard_ratio_pct_std": statistics.stdev(csr) if len(csr) > 1 else 0.0,
            "composite_score_mean": statistics.fmean(cmp),
            "composite_score_std": statistics.stdev(cmp) if len(cmp) > 1 else 0.0,
            "relationship_inference_pct_mean": statistics.fmean(rip),
            "relationship_inference_pct_std": statistics.stdev(rip) if len(rip) > 1 else 0.0,
            "n_runs": len(rep_seeds),
            "seeds": ";".join(str(s) for s in rep_seeds),
        })
    write_csv(out / "frontier_repeats.csv", rep_rows,
              ["w", "load_std_pp_mean", "load_std_pp_std",
               "cross_shard_ratio_pct_mean", "cross_shard_ratio_pct_std",
               "composite_score_mean", "composite_score_std",
               "relationship_inference_pct_mean", "relationship_inference_pct_std",
               "n_runs", "seeds"])
    return ["frontier.csv", "frontier_baselines.csv", "frontier_repeats.csv"]

def exp_ablation(args, out: Path, ctx) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    variants = [
        ("full", {}),
        ("no_epoch_rebalance", {"rebalance_groups": False}),
        ("no_descending_packing", {"descending_packing": False}),
        ("no_intra_epoch_counter", {"intra_epoch_counter": False}),
        ("no_rebalance_and_no_counter", {"rebalance_groups": False, "intra_epoch_counter": False}),
    ]
    rows = []
    for variant, kw in variants:
        st = EnhancedStrategy(args.shards, groups, seed=args.seed,
                              predictor=args.predictor,
                              intra_epoch_feedback=args.intra_epoch_feedback,
                              epsilon_base=args.epsilon, **kw)
        res = run_one(st, epochs, model, verbose=False)
        row = summarize_row(variant, res, st)
        rel = relation_pct(res, true_groups, eval_pairs, args.seed)
        rows.append({
            "variant": variant,
            "cross_shard_ratio_pct": row["cross_shard_ratio_pct"],
            "load_std_pp": row["load_std_pp"],
            "load_cv_pct": row["load_cv_pct"],
            "relationship_inference_pct": rel["accuracy"] * 100,
            "composite_score": row["composite_score"],
        })
    write_csv(out / "ablation_mechanisms.csv", rows,
              ["variant", "cross_shard_ratio_pct", "load_std_pp", "load_cv_pct",
               "relationship_inference_pct", "composite_score"])
    return ["ablation_mechanisms.csv"]

def exp_packing(args, out: Path, ctx) -> list[str]:
    """Compare descending and ascending group packing over feedback weights and repeated seeds."""
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    seeds = args.seeds if args.seeds else [args.seed + i for i in range(args.repeats)]
    rows = []
    for desc in (True, False):
        for w in parse_csv_list(args.packing_ws):
            lss, csr, cmp, rip = [], [], [], []
            for s in seeds:
                st = EnhancedStrategy(args.shards, groups, seed=s,
                                      predictor=args.predictor,
                                      intra_epoch_feedback=w,
                                      epsilon_base=args.epsilon,
                                      descending_packing=desc)
                res = run_one(st, epochs, model, verbose=False)
                row = summarize_row("Enhanced", res, st)
                rel = relation_pct(res, true_groups, eval_pairs, s)
                lss.append(row["load_std_pp"])
                csr.append(row["cross_shard_ratio_pct"])
                cmp.append(row["composite_score"])
                rip.append(rel["accuracy"] * 100)
            rows.append({
                "descending_packing": desc,
                "w": w,
                "load_std_pp_mean": statistics.fmean(lss),
                "load_std_pp_std": statistics.stdev(lss) if len(lss) > 1 else 0.0,
                "cross_shard_ratio_pct_mean": statistics.fmean(csr),
                "cross_shard_ratio_pct_std": statistics.stdev(csr) if len(csr) > 1 else 0.0,
                "relationship_inference_pct_mean": statistics.fmean(rip),
                "relationship_inference_pct_std": statistics.stdev(rip) if len(rip) > 1 else 0.0,
                "composite_score_mean": statistics.fmean(cmp),
                "composite_score_std": statistics.stdev(cmp) if len(cmp) > 1 else 0.0,
                "n_runs": len(seeds),
                "seeds": ";".join(str(x) for x in seeds),
            })
    write_csv(out / "packing_w_grid.csv", rows,
              ["descending_packing", "w",
               "load_std_pp_mean", "load_std_pp_std",
               "cross_shard_ratio_pct_mean", "cross_shard_ratio_pct_std",
               "relationship_inference_pct_mean", "relationship_inference_pct_std",
               "composite_score_mean", "composite_score_std", "n_runs", "seeds"])
    return ["packing_w_grid.csv"]

def _true_readings_permille(args, groups, epochs) -> list[dict[str, int]]:
    """Run the Enhanced baseline to the selected epoch and convert normalized resource readings to the 0..10000 reporting scale."""
    model = ResourceModel()
    st = EnhancedStrategy(args.shards, groups, seed=args.seed,
                          predictor=args.predictor,
                          intra_epoch_feedback=args.intra_epoch_feedback,
                          epsilon_base=args.epsilon)
    res = run_one(st, epochs, model, verbose=False)
    ep = min(args.byz_source_epoch, len(res["per_epoch"]) - 1)
    norm = res["per_epoch"][ep]["norm"]  
    return [{d: round(min(1.0, max(0.0, norm[s][d])) * 10000) for d in DIMS}
            for s in range(args.shards)]

def exp_byz(args, out: Path, seed: int, ctx) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    dims = list(DIMS)
    true_table = _true_readings_permille(args, groups, epochs)
    noise_range = round(10000 * args.honest_noise_pct / 100)

    def honest_reading(si: int, d: str, member: int) -> int:
        rng = random.Random(f"honest:{seed}:{si}:{d}:{member}")
        t = true_table[si][d]
        return max(0, min(10000, t + rng.randint(-noise_range, noise_range)))

    median_rule = "statistics.median (odd: middle element; even: mean of two middle)"

    rows = []
    for m in [4, 5, 6, 7, 9]:
        for f in range(0, (m - 1) // 2 + 1):
            modes = ["upper", "lower", "random"] if f > 0 else ["none"]
            for mode in modes:
                
                rng_l = random.Random(f"liar:{seed}:{m}:{f}:{mode}")
                med_dev, mean_dev, spreads = [], [], []
                shard_loads_obs = []
                for si, truth in enumerate(true_table):
                    dim_loads = []
                    for d in dims:
                        t = truth[d]

                        honest = [honest_reading(si, d, i) for i in range(m - f)]
                        if mode == "upper":
                            liars = [10000] * f
                        elif mode == "lower":
                            liars = [0] * f
                        elif mode == "random":
                            liars = [rng_l.choice([0, 10000]) for _ in range(f)]
                        else:
                            liars = []
                        reports = honest + liars

                        hon_med = statistics.median(honest)
                        hon_mean = statistics.fmean(honest)
                        obs_med = statistics.median(reports)
                        obs_mean = statistics.fmean(reports)
                        if hon_med:
                            med_dev.append(abs(obs_med - hon_med) / hon_med * 100)
                        if hon_mean:
                            mean_dev.append(abs(obs_mean - hon_mean) / hon_mean * 100)
                        if t > 0:
                            spreads.append((max(honest) - min(honest)) / t * 100)
                        dim_loads.append(obs_med)
                    shard_loads_obs.append(statistics.fmean(dim_loads))
                total = sum(shard_loads_obs)
                shares = [v / total * 100 for v in shard_loads_obs] if total else [0] * len(shard_loads_obs)
                rows.append({
                    "committee_size": m,
                    "num_liars": f,
                    "lie_mode": mode,
                    "median_deviation_pct": round(statistics.fmean(med_dev), 6) if med_dev else 0.0,
                    "mean_deviation_pct": round(statistics.fmean(mean_dev), 6) if mean_dev else 0.0,
                    "honest_spread_pct": round(statistics.fmean(spreads), 6) if spreads else 0.0,
                    "median_rule": median_rule,
                    "resulting_load_std_pp": round(statistics.pstdev(shares), 6),
                    "source_epoch": args.byz_source_epoch,
                    "honest_noise_pct": args.honest_noise_pct,
                    "honest_noise_range": noise_range,
                    "seed": seed,
                })
    write_csv(out / "byzantine_median.csv", rows,
              ["committee_size", "num_liars", "lie_mode",
               "median_deviation_pct", "mean_deviation_pct", "honest_spread_pct", "median_rule",
               "resulting_load_std_pp",
               "source_epoch", "honest_noise_pct", "honest_noise_range", "seed"])
    return ["byzantine_median.csv"]

def exp_composite(args, out: Path, ctx) -> tuple[list[str], float | None]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    results = run_all_strategies(args.shards, groups, args.seed, epochs, model,
                                 predictor=args.predictor,
                                 intra_epoch_feedback=args.intra_epoch_feedback,
                                 epsilon_base=args.epsilon)
    rows = []
    w = 0.0
    while w <= 1.000001:
        scores = {}
        for name, (st, res) in results.items():
            row = summarize_row(name, res, st, load_weight=w)
            scores[name] = row["composite_score"]
        winner = max(scores, key=scores.get)
        rows.append({
            "load_weight": round(w, 6),
            "score_enhanced": scores["Enhanced"],
            "score_monoxide": scores["Monoxide"],
            "score_omniledger": scores["OmniLedger"],
            "score_graph": scores["Graph"],
            "winner": winner,
        })
        w = round(w + 0.05, 6)
    write_csv(out / "composite_weight_sweep.csv", rows,
              ["load_weight", "score_enhanced", "score_monoxide",
               "score_omniledger", "score_graph", "winner"])

    def runner_up(r: dict) -> float:
        return max(r["score_monoxide"], r["score_omniledger"], r["score_graph"])

    enhanced_wins = [r for r in rows if r["winner"] == "Enhanced"]
    if not enhanced_wins:
        ub = None
    else:
        last_r = enhanced_wins[-1]
        last_w = last_r["load_weight"]
        next_idx = rows.index(last_r) + 1
        if next_idx >= len(rows):
            ub = 1.0
        else:
            nxt = rows[next_idx]
            g1 = last_r["score_enhanced"] - runner_up(last_r)
            g2 = nxt["score_enhanced"] - runner_up(nxt)
            if g1 > 0 and g2 < 0 and (g1 - g2) > 0:
                w2 = nxt["load_weight"]
                ub = round(last_w + (w2 - last_w) * g1 / (g1 - g2), 6)
            else:
                ub = last_w
    return ["composite_weight_sweep.csv"], ub

def exp_repeats(args, out: Path, ctx, seeds: list[int]) -> list[str]:
    _, _, groups, true_groups, epochs, eval_pairs = ctx
    model = ResourceModel()
    metrics = ["cross_shard_ratio_pct", "load_std_pp", "load_inference_pct",
               "relationship_inference_pct", "composite_score"]
    rec: dict[str, dict[str, list[float]]] = {}
    for s in seeds:
        for strategy in build_strategies(args.shards, groups, s,
                                         predictor=args.predictor,
                                         intra_epoch_feedback=args.intra_epoch_feedback,
                                         epsilon_base=args.epsilon):
            result = run_one(strategy, epochs, model, verbose=False)
            row = summarize_row(strategy.name, result, strategy)
            rel = relation_pct(result, true_groups, eval_pairs, s)
            bucket = rec.setdefault(strategy.name, {mt: [] for mt in metrics})
            bucket["cross_shard_ratio_pct"].append(row["cross_shard_ratio_pct"])
            bucket["load_std_pp"].append(row["load_std_pp"])
            bucket["load_inference_pct"].append(load_infer_pct(result))
            bucket["relationship_inference_pct"].append(rel["accuracy"] * 100)
            bucket["composite_score"].append(row["composite_score"])
    rows = []
    for name, bucket in rec.items():
        for mt in metrics:
            vals = bucket[mt]
            rows.append({
                "strategy": name,
                "metric": mt,
                "mean": statistics.fmean(vals),
                "std": statistics.stdev(vals) if len(vals) > 1 else 0.0,
                "min": min(vals),
                "max": max(vals),
                "n_runs": len(vals),
                "seeds": ";".join(str(s) for s in seeds),
            })
    write_csv(out / "metrics_repeats.csv", rows,
              ["strategy", "metric", "mean", "std", "min", "max", "n_runs", "seeds"])
    return ["metrics_repeats.csv"]

ONCHAIN_REQUIRED = ["onchain_ops.json", "onchain_epoch_gas.json", "onchain_byzantine.json"]

def validate_onchain_inputs(metrics_dir: str) -> list[str]:
    """Validate measurement files and return a list of missing or invalid fields.
Inputs must share a run ID, include operation counts, delay metadata, committee parameters and the required epoch records. An empty list indicates valid input structure."""
    problems: list[str] = []
    base = Path(metrics_dir)
    for name in ONCHAIN_REQUIRED:
        p = base / name
        if not p.exists() or p.stat().st_size == 0:
            problems.append(f"{p}（缺失或为空）")
    if problems:
        return problems

    def load(name: str) -> dict | None:
        try:
            return json.loads((base / name).read_text(encoding="utf-8"))
        except Exception as exc:
            problems.append(f"{name} 解析失败: {exc}")
            return None

    ops = load("onchain_ops.json")
    gas = load("onchain_epoch_gas.json")
    byz = load("onchain_byzantine.json")
    if ops is None or gas is None or byz is None:
        return problems

    run_ids = {}
    for name, obj in (("onchain_ops.json", ops), ("onchain_epoch_gas.json", gas),
                      ("onchain_byzantine.json", byz)):
        rid = obj.get("run_id")
        if not rid:
            problems.append(f"{name}: 缺少 run_id")
        else:
            run_ids[name] = rid
    if len(set(run_ids.values())) != 1:
        problems.append(f"run_id 不一致: {run_ids}")

    netem = ops.get("netem", {})
    if not isinstance(netem, dict) or not netem.get("enabled", False):
        problems.append("onchain_ops.json: netem.enabled=false（必须注入网络时延后采集，）")
    for key in ("delay_ms", "jitter_ms"):
        if key not in netem:
            problems.append(f"onchain_ops.json: netem.{key} 缺失")

    osum = ops.get("summary", {}) or {}
    for key, alt in (("intra_op_count", "intra"), ("cross_op_count", "cross")):
        if key not in ops and alt not in osum:
            problems.append(
                f"onchain_ops.json: 缺少 {key}（也未在 summary.{alt} 中给出）")

    operations = ops.get("operations")
    if not isinstance(operations, list) or not operations:
        problems.append("onchain_ops.json: operations 缺失或为空")
    else:
        committed = [o for o in operations if o.get("status") == "committed"]
        intra = sum(1 for o in committed if o.get("type") == "intra")
        cross = sum(1 for o in committed if o.get("type") == "cross")
        if intra <= 0:
            problems.append("onchain_ops.json: 无已提交的 intra 操作")
        if cross <= 0:
            problems.append("onchain_ops.json: 无已提交的 cross 操作（跨片操作数为 0）")

    epochs_list = gas.get("epochs")
    if not isinstance(epochs_list, list) or not epochs_list:
        problems.append("onchain_epoch_gas.json: epochs 缺失或为空")

    for key in ("committee_size", "num_liars", "lie_mode"):
        if key not in byz:
            problems.append(f"onchain_byzantine.json: 缺少 {key}")
    shards = byz.get("shards")
    if not isinstance(shards, list) or not shards:
        problems.append("onchain_byzantine.json: shards 缺失或为空")

    if isinstance(operations, list) and operations:
        missing_tx = sum(1 for o in operations
                         if not o.get("tx_hash") or o.get("blockNumber") is None)
        if missing_tx:
            problems.append(f"onchain_ops.json: {missing_tx} 笔操作缺少 tx_hash 或 blockNumber")

        gas_vals = {o.get("gas_used") for o in operations
                    if o.get("status") == "committed" and o.get("gas_used") is not None}
        if len(gas_vals) <= 1:
            problems.append("onchain_ops.json: gas_used 取值单一（须为真实业务合约调用，非普通转账）")

        intra_p50 = percentile(op_latencies(ops, "intra"), 50)
        cross_p50 = percentile(op_latencies(ops, "cross"), 50)
        if intra_p50 is not None and cross_p50 is not None and intra_p50 > 0:
            diff_pct = abs(cross_p50 - intra_p50) / intra_p50 * 100
            if diff_pct <= 10:
                problems.append(
                    f"onchain_ops.json: 跨片/片内 p50 时延相差仅 {diff_pct:.2f}%（须 > 10%）")

        t0 = min(o.get("submit_ms", 0) or 0 for o in operations)
        t1 = max(o.get("confirm_ms", 0) or 0 for o in operations)
        actual_span = (t1 - t0) / 1000.0
        declared = float(ops.get("duration_sec", 0) or 0)
        if declared > 0 and actual_span > 0:
            span_diff = abs(declared - actual_span) / declared * 100
            if span_diff > 10:
                problems.append(
                    f"onchain_ops.json: duration_sec={declared} 与实际跨度 {actual_span:.1f}s "
                    f"相差 {span_diff:.1f}%（须 ≤ 10%）")

    if isinstance(epochs_list, list) and epochs_list and len(epochs_list) < 10:
        problems.append(f"onchain_epoch_gas.json: epochs 长度 {len(epochs_list)} < 10")

    return problems

def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    vals = sorted(values)
    idx = max(0, min(len(vals) - 1, int(math.ceil(p / 100 * len(vals))) - 1))
    return vals[idx]

def exp_onchain(args, out: Path, ctx) -> list[str]:
    dataset, summary, groups, true_groups, epochs, eval_pairs = ctx
    base = Path(args.onchain_metrics)
    ops = json.loads((base / "onchain_ops.json").read_text(encoding="utf-8"))
    gas = json.loads((base / "onchain_epoch_gas.json").read_text(encoding="utf-8"))
    byz = json.loads((base / "onchain_byzantine.json").read_text(encoding="utf-8"))

    rows: list[dict] = []
    for otype in ("intra", "cross"):
        lat = op_latencies(ops, otype)
        for name, p in (("p50", 50), ("p95", 95), ("p99", 99)):
            v = percentile(lat, p)
            rows.append({"metric": f"{otype}_{name}_latency_ms",
                         "value": "" if v is None else round(v, 3),
                         "unit": "ms", "source_file": "onchain_ops.json"})
    operations = ops.get("operations", [])
    committed = [o for o in operations if o.get("status") == "committed"]
    aborted = [o for o in operations if o.get("status") == "aborted"]
    duration = float(ops.get("duration_sec", 0) or 0)
    rows.append({"metric": "successful_commit_rate_ops_per_sec",
                 "value": round(len(committed) / duration, 4) if duration else "",
                 "unit": "ops/s", "source_file": "onchain_ops.json"})
    avg_gas = statistics.fmean([o.get("gas_used", 0) for o in committed]) if committed else None
    rows.append({"metric": "avg_gas_per_op",
                 "value": "" if avg_gas is None else round(avg_gas, 2),
                 "unit": "gas", "source_file": "onchain_ops.json"})
    rows.append({"metric": "abort_rate_pct",
                 "value": round(len(aborted) / len(operations) * 100, 6) if operations else 0.0,
                 "unit": "%", "source_file": "onchain_ops.json"})

    osum = ops.get("summary", {}) or {}
    derived = {t: len([o for o in operations if o.get("type") == t]) for t in ("intra", "cross")}
    for t in ("intra", "cross"):
        declared = osum.get(t, ops.get(f"{t}_op_count"))
        if declared is not None and int(declared) != derived[t]:
            raise SystemExit(
                f"onchain_ops.json 不一致：summary.{t}={declared} 与 operations[] 实际 "
                f"{derived[t]} 不符，拒绝产出 onchain_costs.csv")
        rows.append({"metric": f"{t}_op_count", "value": derived[t], "unit": "ops",
                     "source_file": "onchain_ops.json"})
        rows.append({"metric": f"{t}_latency_sample_n",
                     "value": len(op_latencies(ops, t)), "unit": "ops",
                     "source_file": "onchain_ops.json"})
    rows.append({"metric": "total_op_count", "value": len(operations), "unit": "ops",
                 "source_file": "onchain_ops.json"})
    netem = ops.get("netem", {})
    rows.append({"metric": "netem_delay_ms",
                 "value": netem.get("delay_ms", ""), "unit": "ms",
                 "source_file": "onchain_ops.json"})
    rows.append({"metric": "netem_jitter_ms",
                 "value": netem.get("jitter_ms", ""), "unit": "ms",
                 "source_file": "onchain_ops.json"})

    for ep in gas.get("epochs", []):
        ep_idx = ep.get("epoch")
        steps = ep.get("steps", [])
        total = sum(s.get("gas_used", 0) for s in steps)
        rows.append({"metric": f"epoch_{ep_idx}_six_steps_gas_total",
                     "value": total, "unit": "gas", "source_file": "onchain_epoch_gas.json"})
        for s in steps:
            rows.append({"metric": f"epoch_{ep_idx}_step_{s.get('name', '?')}_gas",
                         "value": s.get("gas_used", 0), "unit": "gas",
                         "source_file": "onchain_epoch_gas.json"})

    for sh in byz.get("shards", []):
        hon = sh.get("honest_median")
        obs = sh.get("observed_median")
        dev = ""
        if isinstance(hon, (int, float)) and isinstance(obs, (int, float)) and hon:
            dev = round(abs(obs - hon) / hon * 100, 6)

        rows.append({"metric": f"epoch_{sh.get('epoch', '?')}_shard_{sh.get('shard', '?')}"
                               "_median_deviation_pct",
                     "value": dev, "unit": "%", "source_file": "onchain_byzantine.json"})

    rows.append({"metric": "committee_size",
                 "value": byz.get("committee_size", ""), "unit": "",
                 "source_file": "onchain_byzantine.json"})
    rows.append({"metric": "num_liars",
                 "value": byz.get("num_liars", ""), "unit": "",
                 "source_file": "onchain_byzantine.json"})
    rows.append({"metric": "lie_mode",
                 "value": byz.get("lie_mode", ""), "unit": "",
                 "source_file": "onchain_byzantine.json"})

    write_csv(out / "onchain_costs.csv", rows,
              ["metric", "value", "unit", "source_file"])
    return ["onchain_costs.csv"]

def write_run_manifest(out: Path, args, ctx, produced: set[str]) -> None:
    """Write source hashes, parameters and environment metadata for this run. Git metadata is optional."""
    _, summary, groups, true_groups, epochs, eval_pairs = ctx

    def sha256(path: Path) -> str:
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def git(*cmd: str):
        try:
            top = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True,
                                 text=True, timeout=10, cwd=HERE)
            if top.returncode != 0 or Path(top.stdout.strip()).resolve() != HERE.parent.resolve():
                return None
            p = subprocess.run(["git", *cmd], capture_output=True, text=True, timeout=10, cwd=HERE)
            return p.stdout.strip() if p.returncode == 0 else None
        except (OSError, subprocess.SubprocessError):
            return None

    def ver(cmd: list[str]) -> str:
        try:
            return subprocess.run(cmd, capture_output=True, text=True, timeout=10).stdout.strip()
        except Exception:
            return "n/a"

    cli = {k: v for k, v in vars(args).items() if not k.startswith("_")}
    for key in ("deployed", "rpc", "onchain_metrics", "data"):
        cli.pop(key, None)

    cli["out"] = "outputs/" + Path(args.out).name

    inputs: list[dict] = []
    run_id = "n/a"
    if args.onchain_metrics:
        base = Path(args.onchain_metrics)
        inputs = [{"path": "results/onchain-input/" + n, "sha256": sha256(base / n)}
                  for n in ONCHAIN_REQUIRED if (base / n).exists()]
        try:
            run_id = json.loads((base / "onchain_ops.json").read_text(encoding="utf-8")).get("run_id", "n/a")
        except Exception:
            run_id = "n/a"

    outputs = [p for p in sorted(out.glob("*.csv")) + sorted(out.glob("*.json")) + sorted(out.glob("*.md"))
               if p.name != "run_manifest.json"]
    manifest = {
        "run_id": run_id,
        "git_commit": git("rev-parse", "HEAD"),
        "git_dirty": bool(git("status", "--porcelain")) if git("rev-parse", "HEAD") else None,
        "source_sha256": {p.relative_to(HERE.parent).as_posix(): sha256(p) for p in sorted(HERE.rglob("*.py"))},
        "python_version": platform.python_version(),
        "dependencies": {"pycryptodome": ver([sys.executable, "-c",
                                              "import Crypto; print(Crypto.__version__)"])},
        "toolchain": {
            "solc_version": ver(["solc", "--version"]).splitlines()[0] if ver(["solc", "--version"]) else "n/a",
            "besu_version": ver(["besu", "--version"]).splitlines()[0] if ver(["besu", "--version"]) else "n/a",
        },
        "cli_args": cli,
        "data_files": [{"path": "experiments/data/" + Path(args.data).name,
                        "sha256": summary.get("source_sha256", "n/a")}],
        "inputs": inputs,
        "outputs": [{"path": str(p.relative_to(out)), "sha256": sha256(p)} for p in outputs],
    }
    out.joinpath("run_manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")

def op_latencies(ops: dict, otype: str) -> list[float]:
    vals = []
    for op in ops.get("operations", []):
        if op.get("type") != otype or op.get("status") != "committed":
            continue
        s, c = op.get("submit_ms"), op.get("confirm_ms")
        if s is None or c is None:
            continue
        vals.append(max(0.0, float(c) - float(s)))
    return vals

# ---------------------------------------------------------------- figure_data_map.md


def write_figure_data_map(out: Path, produced: set[str]) -> None:
    """List generated CSV files and their actual column names."""
    lines = ["# Result data map", "", "| Data file | Columns |", "|---|---|"]
    for name in sorted(produced):
        path = out / name
        if path.suffix != ".csv" or not path.is_file():
            continue
        with path.open(encoding="utf-8", newline="") as handle:
            header = next(csv.reader(handle), [])
        lines.append(f"| `{name}` | {', '.join(header)} |")
    out.joinpath("figure_data_map.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

# ---------------------------------------------------------------- analysis.md

def write_analysis(out: Path, args, ctx, produced: set[str], composite_ub: float | None) -> None:
    """Write an English summary of this replay invocation and its outputs."""
    summary = ctx[1]
    lines = [
        "# Replay results", "",
        f"Shards: {args.shards}; seed: {args.seed}; predictor: {args.predictor}; feedback weight: {args.intra_epoch_feedback}.",
        f"Workload SHA-256: `{summary.get('source_sha256', 'n/a')}`.", "",
        "Load and resource summaries exclude epoch 0; cross-shard ratios include all transactions.",
        "Load spread is the population standard deviation of per-shard shares in percentage points.",
        "Enhanced uses prediction-fused load; baseline scheduling metrics use raw weighted load.",
        "Evaluation pairs are shared across strategies. Relationship observations use cumulative assignment snapshots.",
        "The packing-order ablation reverses group ordering while keeping least-loaded-bin placement enabled.", "",
        "## Output files", "",
    ]
    lines.extend(f"- `{name}`" for name in sorted(produced))
    if "composite_weight_sweep.csv" in produced:
        lines += ["", f"Enhanced ranking interval upper bound: {composite_ub}."]
    if "onchain_costs.csv" in produced:
        lines += ["", "Chain costs aggregate the supplied coordination measurements from one logical-shard QBFT chain."]
    out.joinpath("analysis.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

EXPERIMENTS = ["base", "eps", "temporal", "bg", "adapt", "frontier", "packing",
               "ablation", "byz", "composite", "repeats", "onchain"]

def main() -> None:
    parser = argparse.ArgumentParser(description="单链逻辑分片回放 + 链上汇总")
    parser.add_argument("--data", default=str(HERE / "data" / "transactions.txt"))
    parser.add_argument("--out", default=str(HERE.parent / "outputs/onchain"))
    parser.add_argument("--deployed", default=str(HERE.parent / "deployed.json"))
    parser.add_argument("--rpc", default="http://localhost:8545")
    parser.add_argument("--shards", type=int, default=4)
    parser.add_argument("--blocks-per-epoch", type=int, default=100)
    parser.add_argument("--theta", type=float, default=0.15)
    parser.add_argument("--epsilon", type=float, default=0.2,
                        help="负载发布一路的隐私基准 ε（Eq.16，默认 0.2）；"
                             "ε 扫描用 --sweep-epsilons 覆盖")
    parser.add_argument("--seed", type=int, default=20260801)
    parser.add_argument("--limit", type=int, default=None)
    parser.add_argument("--eval-pairs", type=int, default=20000)
    parser.add_argument("--sweep-epsilons", default="0.1,0.2,0.5,0.8,1.2,2.0")
    parser.add_argument("--repeats", type=int, default=5)
    parser.add_argument("--seeds", nargs="*", type=int, default=None)
    parser.add_argument("--spike-epoch", type=int, default=12)
    parser.add_argument("--spike-factor", type=float, default=3.0)
    parser.add_argument("--frontier-weights", default="0:3.0:0.1")
    parser.add_argument("--predictor", default="holt", choices=["holt", "arima", "persistence"],
                        help="负载预测器：holt=霍尔特线性指数平滑，arima=ARIMA(2,1,1)，"
                             "persistence=朴素持久化（上一 epoch 负载）")
    parser.add_argument("--intra-epoch-feedback", type=float, default=1.5,
                        help="Enhanced 策略 epoch 内反馈权重 w（ 前沿扫描的基准操作点）")
    parser.add_argument("--honest-noise-pct", type=float, default=2.0,
                        help="拜占庭实验诚实成员测量噪声幅度（相对真值的百分比，）")
    parser.add_argument("--byz-source-epoch", type=int, default=10,
                        help="拜占庭实验从 ResourceModel 取真值读数的 epoch")
    parser.add_argument("--permutation-runs", type=int, default=None,
                        help="负载推断随机打乱基准的置换次数；默认回落到 --repeats")
    parser.add_argument("--onchain-metrics", default=None,
                        help="JS 侧采集目录（含 onchain_ops/onchain_epoch_gas/onchain_byzantine.json）")
    parser.add_argument("--frontier-repeat-ws", default="1.5,1.6,1.8,2.0,2.4,3.0",
                        help="frontier_repeats 的绝对 w 网格（逗号分隔）；"
                             "当前工作点会自动并入")
    parser.add_argument("--packing-ws", default="1.5,1.8,2.0,2.4",
                        help="packing × w 消融的 w 网格（逗号分隔）")
    parser.add_argument("--only", default=None,
                        help="逗号分隔的实验名: base,eps,temporal,bg,adapt,frontier,ablation,byz,composite,repeats,onchain")
    parser.add_argument("--all", action="store_true", help="运行全部实验（含 onchain）")
    parser.add_argument("--no-chain-check", action="store_true")
    parser.add_argument("--skip-sweep", action="store_true", help="仅开发调试；不再跳过任何产出")
    args = parser.parse_args()

    if args.all and args.only:
        raise SystemExit("--all 与 --only 不能同时使用")
    if args.all:
        selected = list(EXPERIMENTS)
    elif args.only:
        selected = [x.strip() for x in args.only.split(",") if x.strip()]
        unknown = [x for x in selected if x not in EXPERIMENTS]
        if unknown:
            raise SystemExit(f"未知实验名: {', '.join(unknown)}（可选: {', '.join(EXPERIMENTS)}）")
    else:
        selected = ["base", "eps"]
    if args.skip_sweep:
        print("注意: --skip-sweep 仅用于开发调试，epsilon_sweep.csv 仍会完整产出")

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    missing_onchain: list[str] = []
    if "onchain" in selected:
        if not args.onchain_metrics:
            missing_onchain = [f"未提供 --onchain-metrics（需要 {', '.join(ONCHAIN_REQUIRED)}）"]
        else:
            missing_onchain = validate_onchain_inputs(args.onchain_metrics)
        if missing_onchain:
            print("链上数据缺失，相关产出将标记为缺失并以非零状态退出:", file=sys.stderr)
            for item in missing_onchain:
                print(f"  - {item}", file=sys.stderr)

    ctx = load_pipeline(args)
    _, summary, groups, true_groups, epochs, eval_pairs = ctx

    produced: set[str] = set()
    composite_ub: float | None = None
    if "base" in selected:
        print("==> base")
        produced.update(exp_base(args, out, ctx))
    if "eps" in selected:
        print("==> eps")
        produced.update(exp_eps(args, out, ctx, parse_csv_list(args.sweep_epsilons)))
    if "temporal" in selected:
        print("==> temporal ()")
        produced.update(exp_temporal(args, out, ctx))
    if "bg" in selected:
        print("==> bg ()")
        produced.update(exp_bg(args, out, ctx))
    if "adapt" in selected:
        print("==> adapt ()")
        produced.update(exp_adapt(args, out, ctx))
    if "frontier" in selected:
        print("==> frontier ()")
        produced.update(exp_frontier(args, out, ctx, parse_csv_range(args.frontier_weights)))
    if "ablation" in selected:
        print("==> ablation ()")
        produced.update(exp_ablation(args, out, ctx))
    if "packing" in selected:
        print("==> packing ()")
        produced.update(exp_packing(args, out, ctx))
    if "byz" in selected:
        print("==> byz ()")
        produced.update(exp_byz(args, out, args.seed, ctx))
    if "composite" in selected:
        print("==> composite ()")
        files, composite_ub = exp_composite(args, out, ctx)
        produced.update(files)
    if "repeats" in selected:
        print("==> repeats ()")
        seeds = args.seeds if args.seeds else [args.seed + i for i in range(args.repeats)]
        produced.update(exp_repeats(args, out, ctx, seeds))
    if "onchain" in selected and not missing_onchain:
        print("==> onchain ()")
        produced.update(exp_onchain(args, out, ctx))

    write_figure_data_map(out, produced)
    write_analysis(out, args, ctx, produced, composite_ub)
    produced.add("figure_data_map.md")
    produced.add("analysis.md")
    write_run_manifest(out, args, ctx, produced)
    produced.add("run_manifest.json")

    print(f"==> 完成: {out.resolve()}（产出 {len(produced)} 个文件）")
    if missing_onchain:
        sys.exit(1)

if __name__ == "__main__":
    main()
