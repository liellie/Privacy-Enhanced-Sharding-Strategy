"""Parse Ethereum transaction records and build interaction graphs and affinity groups.
Addresses serve as organizational proxies. Threshold-graph connected components define groups. Optional synthetic interactions affect the observed graph; reference labels use the original trace."""

from __future__ import annotations

import itertools
import math
import random
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path

LINE_RE = re.compile(
    r"Block:\s*(\d+),\s*"
    r"Transaction Hash:\s*(0x[0-9a-fA-F]+),\s*"
    r"From:\s*(0x[0-9a-fA-F]{40}),\s*"
    r"To:\s*(0x[0-9a-fA-F]{40})?,\s*"
    r"Value:\s*(\d+),\s*"
    r"Block Time:\s*(\S+)"
)

@dataclass(slots=True)
class Tx:
    block: int
    tx_hash: str
    sender: str
    receiver: str | None
    value: int
    timestamp: str

class Dataset:
    def __init__(self, txs: list[Tx], fake_ratio: float = 0.0, seed: int = 20260801):
        """Build observed and reference interaction graphs.
Optional fake_ratio adds sender-to-random-address interactions to the observed graph; the reference graph retains the input trace for attack labels. The paper profile uses zero injection."""
        self.txs = txs
        self.fake_ratio = fake_ratio
        self.seed = seed
        self._pair: Counter = Counter()
        self._total: Counter = Counter()
        self._true_pair: Counter = Counter()
        self._true_total: Counter = Counter()
        self._fake_edges = 0
        self._build_counts()

    @classmethod
    def load(cls, path: str | Path, limit: int | None = None,
             fake_ratio: float = 0.0, seed: int = 20260801) -> "Dataset":
        txs: list[Tx] = []
        unparsed = 0
        with open(path, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                m = LINE_RE.match(line)
                if not m:
                    unparsed += 1
                    continue
                block, h, frm, to, val, ts = m.groups()
                txs.append(
                    Tx(int(block), h, frm.lower(), to.lower() if to else None, int(val), ts)
                )
                if limit and len(txs) >= limit:
                    break
        if unparsed:
            print(f"  [dataset] {unparsed} 行无法解析，已跳过")
        return cls(txs, fake_ratio=fake_ratio, seed=seed)

    def _build_counts(self) -> None:
        import random as _r
        rng = _r.Random(self.seed)
        parties = []
        for t in self.txs:
            if t.receiver is None or t.receiver == t.sender:
                self._total[t.sender] += 1
                self._true_total[t.sender] += 1
                parties.append(t.sender)
                continue
            key = (t.sender, t.receiver) if t.sender < t.receiver else (t.receiver, t.sender)
            for pair, tot in ((self._pair, self._total), (self._true_pair, self._true_total)):
                pair[key] += 1
                tot[t.sender] += 1
                tot[t.receiver] += 1
            parties.append(t.sender)
            parties.append(t.receiver)

        if self.fake_ratio <= 0 or not parties:
            return
        pool = list(dict.fromkeys(parties))
        for t in self.txs:
            if t.receiver is None or t.receiver == t.sender:
                continue
            if rng.random() >= self.fake_ratio:
                continue
            other = pool[rng.randrange(len(pool))]
            if other == t.sender:
                continue
            key = (t.sender, other) if t.sender < other else (other, t.sender)
            self._pair[key] += 1
            self._total[t.sender] += 1
            self._total[other] += 1
            self._fake_edges += 1

    @property
    def addresses(self) -> list[str]:
        return sorted(self._total)

    def summary(self) -> dict:
        blocks = [t.block for t in self.txs]
        times = [t.timestamp for t in self.txs]
        top = self._total.most_common(5)
        return {
            "total_transactions": len(self.txs),
            "unique_addresses": len(self._total),
            "unique_senders": len({t.sender for t in self.txs}),
            "unique_receivers": len({t.receiver for t in self.txs if t.receiver}),
            "block_min": min(blocks),
            "block_max": max(blocks),
            "block_span": max(blocks) - min(blocks) + 1,
            "time_min": min(times),
            "time_max": max(times),
            "top5_activity": [c for _, c in top],
            "top5_addresses": [a for a, _ in top],
            "interacting_pairs": len(self._pair),
        }

    def power_law_exponent(self) -> float:
        """Return a Hill tail estimate of address activity; this diagnostic is not a goodness-of-fit test."""
        counts = sorted((c for c in self._total.values() if c > 0), reverse=True)
        k = max(10, len(counts) // 100)
        if k >= len(counts) or counts[k] <= 0:
            return float("nan")
        xmin = counts[k]
        tail = [c for c in counts[:k] if c > 0]
        s = sum(math.log(c / xmin) for c in tail)
        return 1.0 + len(tail) / s if s > 0 else float("nan")

    def affinity(self, a: str, b: str) -> float:
        key = (a, b) if a < b else (b, a)
        lab = self._pair.get(key, 0)
        if lab == 0:
            return 0.0
        la, lb = self._total[a], self._total[b]
        if la == 0 or lb == 0:
            return 0.0
        return lab / math.sqrt(la * lb)

    def affinity_edges(self, theta: float) -> list[tuple[str, str, float]]:
        out = []
        for (a, b), _ in self._pair.items():
            v = self.affinity(a, b)
            if v >= theta:
                out.append((a, b, v))
        return out

    def affinity_groups(self, theta: float) -> dict[str, int]:
        """Return connected components of the affinity threshold graph. Group IDs start at one; zero denotes an ungrouped address."""
        parent: dict[str, str] = {}

        def find(x: str) -> str:
            parent.setdefault(x, x)
            root = x
            while parent[root] != root:
                root = parent[root]
            while parent[x] != root:
                parent[x], x = root, parent[x]
            return root

        def union(x: str, y: str) -> None:
            rx, ry = find(x), find(y)
            if rx == ry:
                return
            
            if rx < ry:
                parent[ry] = rx
            else:
                parent[rx] = ry

        for a, b, _ in self.affinity_edges(theta):
            union(a, b)

        roots: dict[str, int] = {}
        groups: dict[str, int] = {}
        for node in parent:
            r = find(node)
            if r not in roots:
                roots[r] = len(roots) + 1
            groups[node] = roots[r]
        return groups

    def true_affinity(self, a: str, b: str) -> float:
        key = (a, b) if a < b else (b, a)
        lab = self._true_pair.get(key, 0)
        if lab == 0:
            return 0.0
        la, lb = self._true_total[a], self._true_total[b]
        if la == 0 or lb == 0:
            return 0.0
        return lab / math.sqrt(la * lb)

    def true_affinity_groups(self, theta: float) -> dict[str, int]:
        """Construct reference relationship labels using only interactions present in the input trace."""
        saved_pair, saved_total = self._pair, self._total
        self._pair, self._total = self._true_pair, self._true_total
        try:
            return self.affinity_groups(theta)
        finally:
            self._pair, self._total = saved_pair, saved_total

    def group_stats(self, groups: dict[str, int]) -> dict:
        sizes = Counter(groups.values())
        grouped = len(groups)
        return {
            "num_groups": len(sizes),
            "grouped_addresses": grouped,
            "ungrouped_addresses": len(self._total) - grouped,
            "largest_group": max(sizes.values()) if sizes else 0,
            "mean_group_size": (grouped / len(sizes)) if sizes else 0.0,
        }

    def activity_stats(self, groups: dict[str, int]) -> dict:
        """Count transaction activity by the sender's affinity group.
Transactions with an ungrouped sender contribute to ungrouped activity; values are transaction counts, not address counts."""
        total = len(self.txs)
        group_tx: Counter = Counter()
        ungrouped = 0
        for t in self.txs:
            g = groups.get(t.sender, 0)
            if g == 0:
                ungrouped += 1
            else:
                group_tx[g] += 1
        if total == 0:
            return {
                "largest_group_activity_pct": 0.0,
                "grouped_activity_pct": 0.0,
                "ungrouped_activity_pct": 0.0,
                "top10_group_activity_pct": 0.0,
            }
        top10 = sum(v for _, v in group_tx.most_common(10))
        return {
            "largest_group_activity_pct": (max(group_tx.values()) / total * 100) if group_tx else 0.0,
            "grouped_activity_pct": (total - ungrouped) / total * 100,
            "ungrouped_activity_pct": ungrouped / total * 100,
            "top10_group_activity_pct": top10 / total * 100,
        }

    def epoch_appearances(self, blocks_per_epoch: int) -> Counter:
        """Count epochs containing each address directly from the trace, independently of placement history."""
        per_epoch: dict[int, set] = defaultdict(set)
        base = min(t.block for t in self.txs)
        for t in self.txs:
            ep = (t.block - base) // blocks_per_epoch
            per_epoch[ep].add(t.sender)
            if t.receiver:
                per_epoch[ep].add(t.receiver)
        appearances: Counter = Counter()
        for members in per_epoch.values():
            for a in members:
                appearances[a] += 1
        return appearances

    def eval_candidates(self, blocks_per_epoch: int, min_epochs: int) -> list[str]:
        appearances = self.epoch_appearances(blocks_per_epoch)
        return sorted(a for a, c in appearances.items() if c >= min_epochs)

    def sample_eval_pairs(
        self,
        true_groups: dict[str, int],
        n_pairs: int,
        seed: int = 20260801,
        blocks_per_epoch: int = 100,
    ) -> tuple[list[tuple[str, str, int]], dict]:
        """Sample a fixed balanced set shared by all strategies.
Positive pairs share a reference affinity group; negatives cross groups or include ungrouped addresses. Each address must appear in at least one epoch. Raise an error if either class cannot supply the requested size. Attack observations follow cumulative assignment snapshots."""
        n_epochs = len(self.split_epochs(blocks_per_epoch))
        rng = random.Random(seed)

        n_half = max(1, n_pairs // 2)

        by_group: dict[int, list[str]] = defaultdict(list)
        for a, g in true_groups.items():
            if g != 0:
                by_group[g].append(a)
        same: list[tuple[str, str]] = []
        for members in by_group.values():
            if len(members) < 2:
                continue
            for pair in itertools.combinations(members, 2):
                same.append(pair)
                if len(same) >= n_half:
                    break
            if len(same) >= n_half:
                break
        if len(same) < n_half:
            raise RuntimeError(
                f"评估对抽样失败：正样本（真实同组对）可用 {len(same)} 对，"
                f"不足抽样规模 {n_half}。请调小 --eval-pairs。"
            )
        same = same[:n_half]

        pool = sorted(set(true_groups) | {t.sender for t in self.txs} |
                      {t.receiver for t in self.txs if t.receiver})
        diff: list[tuple[str, str]] = []
        guard = 0
        while len(diff) < n_half and guard < n_half * 500:
            a, b = rng.sample(pool, 2)
            guard += 1
            ga, gb = true_groups.get(a, 0), true_groups.get(b, 0)
            if ga == 0 or gb == 0 or ga != gb:
                diff.append((a, b))
        if len(diff) < n_half:
            raise RuntimeError(
                f"评估对抽样失败：负样本（不同组对）可用 {len(diff)} 对，"
                f"不足抽样规模 {n_half}。请调小 --eval-pairs。"
            )

        pairs = [(a, b, 1) for a, b in same] + [(a, b, 0) for a, b in diff]
        meta = {
            "n_pairs": len(pairs),
            "n_pos": len(same),
            "n_neg": len(diff),
            "positive_base_rate_pct": len(same) / len(pairs) * 100,
            "n_epochs": n_epochs,
            "min_epochs": 1,
            "n_candidates": len(pool),
        }
        return pairs, meta

    def split_epochs(self, blocks_per_epoch: int) -> list[list[Tx]]:
        by_epoch: dict[int, list[Tx]] = defaultdict(list)
        base = min(t.block for t in self.txs)
        for t in self.txs:
            by_epoch[(t.block - base) // blocks_per_epoch].append(t)
        return [by_epoch[k] for k in sorted(by_epoch)]
