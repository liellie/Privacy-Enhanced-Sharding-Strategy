#!/usr/bin/env python3
"""Compare four placement strategies and generate CSV tables, LaTeX tables and plots.
All outputs are written to the selected output directory."""

from __future__ import annotations

import argparse
import json
import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from src.attacks import load_inference_accuracy, relationship_inference_accuracy
from src.dataset import Dataset
from src.privacy import WAD, PrivacyBudget
from src.strategies import (
    DIMS, EnhancedStrategy, GraphStrategy, MonoxideStrategy,
    OmniLedgerStrategy, ResourceModel,
)

def build_strategies(k, groups, seed, predictor="persistence", intra_epoch_feedback=1.5,
                     epsilon_base=0.2):
    return [
        EnhancedStrategy(k, groups, seed=seed, predictor=predictor,
                         intra_epoch_feedback=intra_epoch_feedback,
                         epsilon_base=epsilon_base),
        MonoxideStrategy(k, seed=seed, predictor=predictor),
        OmniLedgerStrategy(k, seed=seed, predictor=predictor),
        GraphStrategy(k, seed=seed, predictor=predictor),
    ]

def run_one(strategy, epochs, model, verbose=True):
    """Replay a strategy over the supplied transaction sequence and return epoch metrics."""
    per_epoch = []
    assignment_history = []
    total_tx = total_cross = 0

    for ep_idx, txs in enumerate(epochs):
        strategy.begin_epoch(ep_idx, {"cross_ratio": (total_cross / total_tx) if total_tx else 0.0})

        if isinstance(strategy, GraphStrategy):
            for t in txs:
                if t.receiver and t.receiver != t.sender:
                    strategy.observe(t.sender, t.receiver)

        ep_tx = ep_cross = 0
        for t in txs:
            shard = strategy.assign(t.sender, t)
            if t.receiver and t.receiver != t.sender:
                strategy.assign(t.receiver, t)
            cross = strategy.record(shard, t.sender, t.receiver, model)
            ep_tx += 1
            ep_cross += int(cross)

        total_tx += ep_tx
        total_cross += ep_cross

        strategy.end_epoch(ep_idx, model, {"cross_ratio": (total_cross / total_tx) if total_tx else 0.0})

        loads = [s.load for s in strategy.shards]
        per_epoch.append({
            "epoch": ep_idx,
            "n_tx": ep_tx,
            "n_cross": ep_cross,
            "cross_ratio": ep_cross / ep_tx if ep_tx else 0.0,
            "loads": loads,
            "published": [s.published_load for s in strategy.shards],
            "norm": [dict(s.norm) for s in strategy.shards],
        })
        assignment_history.append(dict(strategy.assignment))

        if verbose and (ep_idx + 1) % 5 == 0:
            print(f"    epoch {ep_idx+1}/{len(epochs)}  交易 {total_tx}  跨片率 "
                  f"{total_cross/total_tx*100:.2f}%")

    return {
        "per_epoch": per_epoch,
        "assignment_history": assignment_history,
        "total_tx": total_tx,
        "total_cross": total_cross,
        "cross_ratio": total_cross / total_tx if total_tx else 0.0,
    }

def summarize(name, res, strategy):
    """Aggregate epoch series into placement, load and resource metrics."""
    stable = res["per_epoch"][1:] or res["per_epoch"]     

    # Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
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

    locality_score = max(0.0, 100.0 - res["cross_ratio"] * 100.0)
    balance_score = max(0.0, 100.0 - 10.0 * load_pp)
    composite = 0.4 * balance_score + 0.6 * locality_score

    row = {
        "strategy": name,
        "total_tx": res["total_tx"],
        "cross_shard_ops": res["total_cross"],
        "cross_shard_ratio_pct": res["cross_ratio"] * 100,
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
    return row

def epsilon_sweep(k, groups, epochs, model, seed, eps_values):
    """Rerun the complete replay for each load epsilon value and collect measured metrics."""
    rows = []
    for eps in eps_values:
        st = EnhancedStrategy(k, groups, seed=seed)
        st.budget = PrivacyBudget(epsilon_base_wad=int(eps * WAD))
        res = run_one(st, epochs, model, verbose=False)
        pub = [e["published"] for e in res["per_epoch"]]
        tru = [e["loads"] for e in res["per_epoch"]]
        rel = relationship_inference_accuracy(res["assignment_history"], groups, seed=seed)
        rows.append({
            "epsilon": eps,
            "relationship_inference_pct": rel["accuracy"] * 100,
            "load_inference_pct": load_inference_accuracy(pub, tru) * 100,
            "cross_shard_ratio_pct": res["cross_ratio"] * 100,
            "budget_exhausted_events": st.budget.exhausted_events,
            "publish_events": st.budget.publish_events,
        })
        print(f"    eps={eps:<5} 关系推断={rows[-1]['relationship_inference_pct']:.1f}%  "
              f"负载推断={rows[-1]['load_inference_pct']:.1f}%  "
              f"跨片={rows[-1]['cross_shard_ratio_pct']:.1f}%")
    return rows

def main():
    from src.report import (make_figures, write_csv, write_latex_table2, write_latex_table3, write_latex_table4)
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data/transactions.txt")
    ap.add_argument("--shards", type=int, default=4)
    ap.add_argument("--blocks-per-epoch", type=int, default=100)
    ap.add_argument("--theta", type=float, default=0.15, help="亲和度阈值 theta_A")
    ap.add_argument("--limit", type=int, default=None, help="只用前 N 笔交易")
    ap.add_argument("--fake-ratio", type=float, default=0.0,
                    help="虚假交互注入比例；实测对关系推断无改善且损害局部性，默认关闭")
    ap.add_argument("--seed", type=int, default=20260801)
    ap.add_argument("--out", default="results")
    ap.add_argument("--skip-sweep", action="store_true")
    args = ap.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    print("=" * 70)
    print("  分片策略对比实验")
    print("=" * 70)

    print("\n[1/6] 载入数据集")
    ds = Dataset.load(args.data, limit=args.limit, fake_ratio=args.fake_ratio, seed=args.seed)
    summary = ds.summary()
    summary["power_law_exponent"] = ds.power_law_exponent()
    for key in ("total_transactions", "unique_addresses", "block_span",
                "time_min", "time_max", "top5_activity", "power_law_exponent"):
        print(f"    {key:22s} {summary[key]}")

    print("\n[2/6] 构建亲和图与分组（Eq.1 + Definition IV.2）")
    groups = ds.affinity_groups(args.theta)
    true_groups = ds.true_affinity_groups(args.theta)
    gs = ds.group_stats(groups)
    summary["affinity"] = {"theta": args.theta, **gs}
    for key, val in gs.items():
        print(f"    {key:22s} {val}")
    (out / "dataset_summary.json").write_text(
        json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")

    print("\n[3/6] 切分 epoch")
    epochs = ds.split_epochs(args.blocks_per_epoch)
    print(f"    {len(epochs)} 个 epoch，每个 {args.blocks_per_epoch} 区块，"
          f"平均 {sum(len(e) for e in epochs)/len(epochs):.0f} 笔/epoch")

    model = ResourceModel()

    print("\n[4/6] 跑四种策略")
    rows, privacy_rows, results = [], [], {}
    for st in build_strategies(args.shards, groups, args.seed):
        print(f"  -> {st.name}")
        res = run_one(st, epochs, model)
        results[st.name] = (st, res)
        rows.append(summarize(st.name, res, st))

        pub = [e["published"] for e in res["per_epoch"]]
        tru = [e["loads"] for e in res["per_epoch"]]
        rel = relationship_inference_accuracy(res["assignment_history"], true_groups, seed=args.seed)
        rel_bg = relationship_inference_accuracy(
            res["assignment_history"], true_groups, known_fraction=0.6, seed=args.seed)
        privacy_rows.append({
            "strategy": st.name,
            "load_inference_pct": load_inference_accuracy(pub, tru) * 100,
            "relationship_inference_pct": rel["accuracy"] * 100,
            "relationship_precision_pct": rel["precision"] * 100,
            "relationship_recall_pct": rel["recall"] * 100,
            "relationship_inference_bg60_pct": rel_bg["accuracy"] * 100,
            "pairs_evaluated": rel["pairs"],
        })
        print(f"     跨片率 {rows[-1]['cross_shard_ratio_pct']:.2f}%   "
              f"负载 CV {rows[-1]['load_cv_pct']:.2f}% / 标准差 {rows[-1]['load_std_pp']:.2f}pp   "
              f"关系推断 {privacy_rows[-1]['relationship_inference_pct']:.1f}%")

    write_csv(out / "metrics.csv", rows)
    write_csv(out / "privacy.csv", privacy_rows)
    write_csv(out / "resource_dims.csv", [
        {"strategy": r["strategy"],
         **{f"std_{d}_pct": r[f"std_{d}_pct"] for d in DIMS},
         **{f"par_{d}": r[f"par_{d}"] for d in DIMS}}
        for r in rows
    ])

    print("\n[5/6] epsilon 影响分析")
    sweep = []
    if not args.skip_sweep:
        sweep = epsilon_sweep(args.shards, groups, epochs, model, args.seed,
                              [0.1, 0.5, 0.8, 1.2, 2.0])
        write_csv(out / "epsilon_sweep.csv", sweep)

    print("\n[5b] 消融：粘性映射 vs 每 epoch 重装箱")
    ablation = []
    for label, reb in (("Sticky mapping", False), ("Per-epoch rebalance", True)):
        st = EnhancedStrategy(args.shards, groups, seed=args.seed, rebalance_groups=reb)
        res = run_one(st, epochs, model, verbose=False)
        mm = summarize("Enhanced", res, st)
        rel = relationship_inference_accuracy(res["assignment_history"], true_groups, seed=args.seed)
        ablation.append({
            "variant": label,
            "cross_shard_ratio_pct": mm["cross_shard_ratio_pct"],
            "load_std_pp": mm["load_std_pp"],
            "load_cv_pct": mm["load_cv_pct"],
            "relationship_inference_pct": rel["accuracy"] * 100,
        })
        print(f"    {label:<22} 跨片 {mm['cross_shard_ratio_pct']:.2f}%  "
              f"负载 {mm['load_std_pp']:.2f}pp  关系推断 {rel['accuracy']*100:.1f}%")
    write_csv(out / "ablation_rebalance.csv", ablation)

    print("\n[6/6] 生成图表与 LaTeX 表")
    write_latex_table2(out / "privacy_table.tex", rows, privacy_rows)
    if sweep:
        write_latex_table3(out / "epsilon_table.tex", sweep)
    write_latex_table4(out / "dataset_table.tex", summary)
    make_figures(out, rows, privacy_rows, results)

    print(f"\n完成。全部结果在 {out.resolve()}")
    print("结果定义与数据映射见 docs/RESULTS_MAP.md。")

if __name__ == "__main__":
    main()
