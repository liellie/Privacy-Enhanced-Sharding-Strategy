"""CSV, LaTeX and plot generation. Figures use a 3.5-inch column width, 8-point fonts and 300 dpi."""

from __future__ import annotations

import csv
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

from .strategies import DIMS

plt.rcParams.update({
    "font.size": 8,
    "axes.labelsize": 8,
    "axes.titlesize": 9,
    "xtick.labelsize": 7,
    "ytick.labelsize": 7,
    "legend.fontsize": 7,
    "figure.dpi": 300,
    "savefig.bbox": "tight",
    "axes.grid": True,
    "grid.alpha": 0.3,
    "grid.linewidth": 0.5,
})

COLORS = {
    "Enhanced": "#2c6fbb",
    "Monoxide": "#c0504d",
    "OmniLedger": "#e8a33d",
    "Graph": "#4f9153",
}
ORDER = ["Enhanced", "Monoxide", "OmniLedger", "Graph"]

def write_csv(path: Path, rows: list[dict]) -> None:
    if not rows:
        return
    keys = list(rows[0])
    with open(path, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=keys)
        w.writeheader()
        for r in rows:
            w.writerow({k: (f"{v:.4f}" if isinstance(v, float) else v) for k, v in r.items()})
    print(f"    {path.name}")

def _order(rows):
    idx = {r["strategy"]: r for r in rows}
    return [idx[n] for n in ORDER if n in idx]

# ---------------------------------------------------------------- LaTeX

def write_latex_table2(path: Path, metrics: list[dict], privacy: list[dict]) -> None:
    """Write a privacy comparison table from measured inference, precision and recall values."""
    pm = {r["strategy"]: r for r in privacy}
    lines = [
        r"\begin{table}[!t]",
        r"\caption{Comparison on privacy protection capability}",
        r"\label{tab:privacy}",
        r"\centering",
        r"\begin{tabular}{lcccc}",
        r"\toprule",
        r"Sharding strategy & \makecell{Load inference\\accuracy} & "
        r"\makecell{Relationship\\inference accuracy} & Precision & Recall \\",
        r"\midrule",
    ]
    for r in _order(metrics):
        p = pm[r["strategy"]]
        lines.append(
            f"{r['strategy']} & {p['load_inference_pct']:.1f}\\% & "
            f"{p['relationship_inference_pct']:.1f}\\% & "
            f"{p['relationship_precision_pct']:.1f}\\% & "
            f"{p['relationship_recall_pct']:.1f}\\% \\\\"
        )
    lines += [r"\bottomrule", r"\end{tabular}", r"\end{table}"]
    path.write_text("\n".join(lines), encoding="utf-8")
    print(f"    {path.name}")

def write_latex_table3(path: Path, sweep: list[dict]) -> None:
    """Write epsilon-sweep results from independently executed replay runs."""
    lines = [
        r"\begin{table}[!t]",
        r"\caption{Differential privacy parameter ($\varepsilon$) impact analysis}",
        r"\label{tab:epsilon}",
        r"\centering",
        r"\begin{tabular}{ccccc}",
        r"\toprule",
        r"$\varepsilon$ & \makecell{Relationship\\inference} & \makecell{Load\\inference} & "
        r"\makecell{Cross-shard\\ratio} & \makecell{Budget\\exhaustions} \\",
        r"\midrule",
    ]
    for r in sweep:
        lines.append(
            f"{r['epsilon']} & {r['relationship_inference_pct']:.1f}\\% & "
            f"{r['load_inference_pct']:.1f}\\% & "
            f"{r['cross_shard_ratio_pct']:.1f}\\% & {r['budget_exhausted_events']} \\\\"
        )
    lines += [r"\bottomrule", r"\end{tabular}", r"\end{table}"]
    path.write_text("\n".join(lines), encoding="utf-8")
    print(f"    {path.name}")

def write_latex_table4(path: Path, summary: dict) -> None:
    """Write dataset characteristics computed from the supplied trace."""
    rows = [
        ("Data source", "Ethereum Mainnet"),
        ("Total transactions", f"{summary['total_transactions']:,}"),
        ("Unique addresses", f"{summary['unique_addresses']:,}"),
        ("Unique senders", f"{summary['unique_senders']:,}"),
        ("Unique receivers", f"{summary['unique_receivers']:,}"),
        ("Block range", f"{summary['block_min']}--{summary['block_max']} ({summary['block_span']} blocks)"),
        ("Time span", f"{summary['time_min']} to {summary['time_max']}"),
        ("Interacting address pairs", f"{summary['interacting_pairs']:,}"),
        ("Top-5 address activity", ", ".join(f"{c:,}" for c in summary["top5_activity"])),
        ("Power-law exponent (Hill)", f"{summary['power_law_exponent']:.2f}"),
    ]
    lines = [
        r"\begin{table}[!t]",
        r"\caption{Ethereum dataset characteristics}",
        r"\label{tab:dataset}",
        r"\centering",
        r"\begin{tabular}{ll}",
        r"\toprule",
        r"Attribute & Value \\",
        r"\midrule",
    ]
    lines += [f"{k} & {v} \\\\" for k, v in rows]
    lines += [r"\bottomrule", r"\end{tabular}", r"\end{table}"]
    path.write_text("\n".join(lines), encoding="utf-8")
    print(f"    {path.name}")

def make_figures(out: Path, metrics: list[dict], privacy: list[dict], results: dict) -> None:
    rows = _order(metrics)
    names = [r["strategy"] for r in rows]
    colors = [COLORS[n] for n in names]

    fig, ax = plt.subplots(figsize=(3.5, 2.4))
    vals = [r["load_std_pp"] for r in rows]
    ax.bar(names, vals, color=colors, width=0.6)
    for i, v in enumerate(vals):
        ax.text(i, v, f"{v:.1f}", ha="center", va="bottom", fontsize=7)
    ax.set_ylabel("Load standard deviation (pp)")
    ax.set_title("Lower is better")
    ax.set_ylim(0, max(vals) * 1.25 if vals else 1)
    fig.savefig(out / "load_std.png")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(3.5, 2.4))
    x = np.arange(len(DIMS))
    w = 0.2
    for i, r in enumerate(rows):
        ax.bar(x + (i - 1.5) * w, [r[f"std_{d}_pct"] for d in DIMS],
               w, label=r["strategy"], color=COLORS[r["strategy"]])
    ax.set_xticks(x)
    ax.set_xticklabels([d.upper() for d in DIMS])
    ax.set_ylabel("Resource std. dev. (%)")
    ax.legend(ncol=2, frameon=False)
    fig.savefig(out / "resource_dims.png")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(3.5, 2.4))
    vals = [r["cross_shard_ratio_pct"] for r in rows]
    ax.bar(names, vals, color=colors, width=0.6)
    for i, v in enumerate(vals):
        ax.text(i, v, f"{v:.1f}", ha="center", va="bottom", fontsize=7)
    ax.set_ylabel("Cross-shard operations (%)")
    ax.set_title("Lower is better")
    ax.set_ylim(0, max(vals) * 1.25 if vals else 1)
    fig.savefig(out / "cross_shard.png")
    plt.close(fig)

    pm = {r["strategy"]: r for r in privacy}
    axes_labels = ["Data locality", "Load balance", "Privacy\n(load)", "Privacy\n(relation)", "Composite"]
    fig, ax = plt.subplots(figsize=(3.5, 3.2), subplot_kw={"projection": "polar"})
    ang = np.linspace(0, 2 * np.pi, len(axes_labels), endpoint=False).tolist()
    ang += ang[:1]
    for r in rows:
        p = pm[r["strategy"]]
        v = [
            max(0.0, 100 - r["cross_shard_ratio_pct"]),
            max(0.0, 100 - r["load_std_pp"] * 5),
            max(0.0, 100 - p["load_inference_pct"]),
            max(0.0, 100 - p["relationship_inference_pct"]),
            r["composite_score"],
        ]
        v += v[:1]
        ax.plot(ang, v, linewidth=1.2, label=r["strategy"], color=COLORS[r["strategy"]])
        ax.fill(ang, v, alpha=0.08, color=COLORS[r["strategy"]])
    ax.set_xticks(ang[:-1])
    ax.set_xticklabels(axes_labels)
    ax.set_ylim(0, 100)
    ax.legend(loc="upper right", bbox_to_anchor=(1.25, 1.12), frameon=False)
    fig.savefig(out / "strategy_radar.png")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(3.5, 2.4))
    for name in ORDER:
        if name not in results:
            continue
        _, res = results[name]
        ax.plot([e["epoch"] for e in res["per_epoch"]],
                [e["cross_ratio"] * 100 for e in res["per_epoch"]],
                linewidth=1.0, label=name, color=COLORS[name])
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Cross-shard ratio (%)")
    ax.legend(ncol=2, frameon=False)
    fig.savefig(out / "fig_cross_shard_over_time.png")
    plt.close(fig)

    print("    load_std.png / resource_dims.png / cross_shard.png "
          "/ strategy_radar.png / fig_cross_shard_over_time.png")
