"""Inference estimators for published loads and cumulative assignment histories.
The modeled observer sees released summaries and placements; full-node visibility is described in docs/IMPLEMENTATION_SCOPE.md."""

from __future__ import annotations

import itertools
import random
from collections import defaultdict

def load_inference_accuracy(published: list[list[float]], truth: list[list[float]],
                            n_bins: int = 10) -> float:
    """Use each published value as a direct estimate of the corresponding load.
Score whether estimate and truth occupy the same equal-width bin between that epoch's true minimum and maximum. The unperturbed baselines publish their load values directly."""
    if not published or not truth:
        return 0.0
    hits = total = 0
    for pub_epoch, true_epoch in zip(published, truth):
        if not true_epoch:
            continue
        lo, hi = min(true_epoch), max(true_epoch)
        span = hi - lo
        for p, t in zip(pub_epoch, true_epoch):
            if span <= 1e-12:
                hits += 1
                total += 1
                continue
            bp = min(n_bins - 1, max(0, int((p - lo) / span * n_bins)))
            bt = min(n_bins - 1, max(0, int((t - lo) / span * n_bins)))
            hits += int(bp == bt)
            total += 1
    return hits / total if total else 0.0

def relationship_inference_accuracy(
    assignment_history: list[dict[str, int]],
    true_groups: dict[str, int],
    known_fraction: float = 0.0,
    max_pairs: int = 20000,
    seed: int = 20260801,
    eval_pairs: list[tuple[str, str, int]] | None = None,
) -> dict:
    """Infer common affinity-group membership from co-location frequency in assignment snapshots.
Use threshold 0.5 without background knowledge; otherwise calibrate it using the known subset and exclude that subset from scoring. Shared eval_pairs keep strategy comparisons on the same pairs. Observations count only snapshots containing both addresses; pairs without observations receive score zero. Return accuracy, precision, recall and evaluated-pair counts."""
    rng = random.Random(seed)

    if eval_pairs is None:
        
        appearances = defaultdict(int)
        for ep in assignment_history:
            for a in ep:
                appearances[a] += 1
        candidates = [a for a, c in appearances.items() if c >= max(2, len(assignment_history) // 2)]
        if len(candidates) < 2:
            return _empty_result()
        same, diff = [], []
        grouped = [a for a in candidates if true_groups.get(a, 0) != 0]
        by_group = defaultdict(list)
        for a in grouped:
            by_group[true_groups[a]].append(a)
        for members in by_group.values():
            if len(members) < 2:
                continue
            for pair in itertools.combinations(members, 2):
                same.append(pair)
                if len(same) >= max_pairs // 2:
                    break
            if len(same) >= max_pairs // 2:
                break
        target = max(1, len(same))
        guard = 0
        while len(diff) < target and guard < target * 50:
            a, b = rng.sample(candidates, 2)
            guard += 1
            ga, gb = true_groups.get(a, 0), true_groups.get(b, 0)
            if ga == 0 or gb == 0 or ga != gb:
                diff.append((a, b))
        if not same or not diff:
            return _empty_result()
        base_pairs = [(a, b, 1) for a, b in same] + [(a, b, 0) for a, b in diff]
    else:
        base_pairs = list(eval_pairs)

    if not base_pairs:
        return _empty_result()

    def colocation(a: str, b: str) -> float:
        both = co = 0
        for ep in assignment_history:
            if a in ep and b in ep:
                both += 1
                if ep[a] == ep[b]:
                    co += 1
        return co / both if both else 0.0

    scored = [(colocation(a, b), y) for a, b, y in base_pairs]
    n_pos = sum(1 for _, y in scored if y == 1)

    if known_fraction > 0:
        n_known = max(1, int(len(scored) * known_fraction))
        known_idx = set(rng.sample(range(len(scored)), n_known))
        known = [scored[i] for i in sorted(known_idx)]
        unknown = [scored[i] for i in range(len(scored)) if i not in known_idx]
        best_t, best_acc = 0.5, -1.0
        for t in [i / 20 for i in range(21)]:
            acc = sum(int((s >= t) == bool(y)) for s, y in known) / len(known)
            if acc > best_acc:
                best_acc, best_t = acc, t
        threshold = best_t
    else:
        threshold = 0.5
        unknown = scored

    tp = sum(1 for s, y in unknown if s >= threshold and y == 1)
    fp = sum(1 for s, y in unknown if s >= threshold and y == 0)
    fn = sum(1 for s, y in unknown if s < threshold and y == 1)
    tn = sum(1 for s, y in unknown if s < threshold and y == 0)
    total = len(unknown)
    n_pos_unknown = sum(1 for _, y in unknown if y == 1)

    return {
        "accuracy": (tp + tn) / total if total else 0.0,
        "precision": tp / (tp + fp) if (tp + fp) else 0.0,
        "recall": tp / (tp + fn) if (tp + fn) else 0.0,
        "pairs": total,
        "pairs_known_removed": len(scored) - total,
        "same_pairs": n_pos_unknown,
        "diff_pairs": total - n_pos_unknown,
        "positive_base_rate_pct": (n_pos_unknown / total * 100) if total else 0.0,
        "threshold": threshold,
    }

def _empty_result() -> dict:
    return {
        "accuracy": 0.0, "precision": 0.0, "recall": 0.0, "pairs": 0,
        "pairs_known_removed": 0, "same_pairs": 0, "diff_pairs": 0,
        "positive_base_rate_pct": 0.0, "threshold": 0.0,
    }
