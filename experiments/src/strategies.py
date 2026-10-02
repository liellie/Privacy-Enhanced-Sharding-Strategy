"""Four placement strategies and a modeled four-dimensional resource workload.
Baselines implement placement adaptations, not the complete consensus systems."""

from __future__ import annotations

import hashlib
from dataclasses import dataclass, field

from .privacy import (
    WAD, PURPOSE_AFFINITY, PrivacyBudget, div_wad, laplace, mul_wad,
)

DIMS = ("cpu", "mem", "stor", "net")

# ======================================================================

# ======================================================================

@dataclass
class ResourceModel:
    """Modeled resource occupancy from transaction and address counts.
CPU and network include cross-shard surcharges, memory follows active addresses, and storage follows resident addresses. Capacities and coefficients below define the model; these are not measured host counters."""

    cpu_per_tx: float = 1.0
    cpu_cross_extra: float = 2.5
    mem_per_active_addr: float = 0.35
    stor_per_addr: float = 0.55
    net_per_tx: float = 1.0
    net_cross_extra: float = 4.0

    capacity: dict[str, float] = field(
        default_factory=lambda: {"cpu": 3000.0, "mem": 2000.0, "stor": 4000.0, "net": 4000.0}
    )

    spike_epoch: int | None = None
    spike_factor: float = 3.0
    spike_shard: int = 0
    spike_ramp_epochs: int = 2

    def _spike_factor_for(self, epoch: int) -> float:
        """Return the linear disturbance ramp, reaching its maximum at spike_epoch.
For ramp=2, factor=3 and spike_epoch=12, epochs 10, 11 and 12 use approximately 1.667, 2.333 and 3."""
        if self.spike_epoch is None or self.spike_factor <= 1.0:
            return 1.0
        if epoch >= self.spike_epoch:
            return self.spike_factor
        if epoch < self.spike_epoch - self.spike_ramp_epochs:
            return 1.0
        k = epoch - (self.spike_epoch - self.spike_ramp_epochs) + 1
        return 1.0 + (self.spike_factor - 1.0) * k / (self.spike_ramp_epochs + 1)

    def epoch_usage(self, n_tx: int, n_cross: int, n_active: int, n_resident: int,
                    epoch: int = 0, shard_id: int = 0) -> dict[str, float]:
        """Compute resource occupancy for a shard and epoch; storage follows its current resident-address set."""
        cpu = self.cpu_per_tx * n_tx + self.cpu_cross_extra * n_cross
        mem = self.mem_per_active_addr * n_active
        stor = self.stor_per_addr * n_resident
        net = self.net_per_tx * n_tx + self.net_cross_extra * n_cross
        usage = {"cpu": cpu, "mem": mem, "stor": stor, "net": net}
        if shard_id == self.spike_shard:
            factor = self._spike_factor_for(epoch)
            if factor != 1.0:
                usage = {d: v * factor for d, v in usage.items()}
        return usage

    def normalize(self, usage: dict[str, float]) -> dict[str, float]:
        """Normalize resource readings to [0,1] using fixed capacities rather than observed window extrema."""
        return {d: min(1.0, max(0.0, usage[d] / self.capacity[d])) for d in DIMS}

# ======================================================================

# ======================================================================

@dataclass
class ShardState:
    shard_id: int
    n_tx: int = 0
    n_cross: int = 0
    resident: set = field(default_factory=set)
    active: set = field(default_factory=set)
    norm: dict = field(default_factory=lambda: {d: 0.0 for d in DIMS})
    load: float = 0.0
    published_load: float = 0.0

    def reset_epoch(self) -> None:
        self.n_tx = 0
        self.n_cross = 0
        self.active = set()

# ======================================================================

# ======================================================================

class Strategy:
    name = "base"
    uses_privacy = False

    def __init__(self, k: int, seed: int = 20260801, predictor: str = "persistence"):
        self.k = k
        self.seed = seed
        self.predictor = predictor
        self.shards = [ShardState(i) for i in range(k)]
        self.assignment: dict[str, int] = {}

        self.predicted: dict[int, float] = {i: 0.0 for i in range(k)}
        self.pred_used_history: list[dict[int, float]] = []
        self.raw_used_history: list[dict[int, float]] = []
        self._holt_level: dict[int, float] = {i: 0.0 for i in range(k)}
        self._holt_trend: dict[int, float] = {i: 0.0 for i in range(k)}
        self._holt_seen: dict[int, bool] = {i: False for i in range(k)}
        self._arima_history: dict[int, list[float]] = {i: [] for i in range(k)}

    def _advance_predictor(self, shard_id: int, raw_load: float) -> float:
        """Update the predictor from current raw load and return the next-epoch forecast."""
        if self.predictor == "holt":
            return self._holt_step(shard_id, raw_load)
        if self.predictor == "arima":
            self._arima_history[shard_id].append(raw_load)
            return self._arima_step(shard_id)
        return raw_load  # persistence

    def _holt_step(self, shard_id: int, raw_load: float) -> float:
        """Perform a Holt linear-trend update with level coefficient 0.5 and trend coefficient 0.3."""
        if not self._holt_seen[shard_id]:
            self._holt_level[shard_id] = raw_load
            self._holt_trend[shard_id] = 0.0
            self._holt_seen[shard_id] = True
        else:
            prev_level = self._holt_level[shard_id]
            prev_trend = self._holt_trend[shard_id]
            new_level = 0.5 * raw_load + 0.5 * (prev_level + prev_trend)
            new_trend = 0.3 * (new_level - prev_level) + 0.7 * prev_trend
            self._holt_level[shard_id] = new_level
            self._holt_trend[shard_id] = new_trend
        return self._holt_level[shard_id] + self._holt_trend[shard_id]

    def _arima_step(self, shard_id: int) -> float:
        """Compute an ARIMA(2,1,1) forecast; use Holt if statsmodels is unavailable or fitting fails.
The paper profile uses Holt. ARIMA floating-point output can depend on platform and library version."""
        series = self._arima_history[shard_id]
        try:
            from statsmodels.tsa.arima.model import ARIMA  # type: ignore
            if len(series) >= 5:
                fitted = ARIMA(series, order=(2, 1, 1)).fit()
                return float(fitted.forecast(steps=1)[0])
        except Exception:
            pass
        return self._holt_step(shard_id, series[-1] if series else 0.0)

    def begin_epoch(self, epoch: int, ctx: dict) -> None:
        for s in self.shards:
            s.reset_epoch()

    def assign(self, addr: str, tx) -> int:
        raise NotImplementedError

    def end_epoch(self, epoch: int, model: ResourceModel, ctx: dict) -> None:
        pred_used: dict[int, float] = {}
        raw_used: dict[int, float] = {}
        for s in self.shards:
            usage = model.epoch_usage(s.n_tx, s.n_cross, len(s.active), len(s.resident),
                                      epoch=epoch, shard_id=s.shard_id)
            s.norm = model.normalize(usage)
            raw_load = self._weighted_load(s.norm)
            raw_used[s.shard_id] = raw_load
            pred_used[s.shard_id] = self.predicted[s.shard_id]
            s.load = raw_load
            self.predicted[s.shard_id] = self._advance_predictor(s.shard_id, raw_load)
            s.published_load = s.load
        self.pred_used_history.append(pred_used)
        self.raw_used_history.append(raw_used)

    def _weighted_load(self, norm: dict[str, float]) -> float:
        return sum(norm[d] * 0.25 for d in DIMS)

    def _move_resident(self, addr: str, shard: int) -> None:
        for s in self.shards:
            if s.shard_id != shard:
                s.resident.discard(addr)
        self.shards[shard].resident.add(addr)

    def record(self, shard: int, addr: str, counterpart: str | None, model: ResourceModel) -> bool:
        """Record a transaction and return whether it crosses shards."""
        s = self.shards[shard]
        s.n_tx += 1
        s.active.add(addr)
        self._move_resident(addr, shard)
        cross = False
        if counterpart is not None:
            other = self.assignment.get(counterpart)
            if other is not None and other != shard:
                cross = True
                s.n_cross += 1
            else:
                s.active.add(counterpart)
        if cross:
            s.n_cross = s.n_cross  
        return cross

# ======================================================================

# ======================================================================

class MonoxideStrategy(Strategy):
    """Address-prefix placement baseline.
Map the highest 32 address bits modulo the shard count; assignments are independent of epoch randomness."""

    name = "Monoxide"

    def assign(self, addr: str, tx) -> int:
        if addr in self.assignment:
            return self.assignment[addr]
        top = int(addr[2:10], 16)  
        s = top % self.k
        self.assignment[addr] = s
        return s

# ======================================================================

# ======================================================================

class OmniLedgerStrategy(Strategy):
    """Epoch-randomized placement baseline.
Hash the epoch seed and address, then reduce modulo the shard count. This models placement reshuffling rather than the complete consensus protocol."""

    name = "OmniLedger"

    def __init__(self, k: int, seed: int = 20260801, reshuffle: bool = True,
                 predictor: str = "persistence"):
        super().__init__(k, seed, predictor=predictor)
        self.reshuffle = reshuffle
        self.epoch_seed = seed

    def begin_epoch(self, epoch: int, ctx: dict) -> None:
        super().begin_epoch(epoch, ctx)
        if self.reshuffle:
            self.epoch_seed = int.from_bytes(
                hashlib.sha3_256(f"{self.seed}:{epoch}".encode()).digest()[:8], "big"
            )
            self.assignment = {}

    def assign(self, addr: str, tx) -> int:
        if addr in self.assignment:
            return self.assignment[addr]
        h = hashlib.sha3_256(f"{self.epoch_seed}:{addr}".encode()).digest()
        s = int.from_bytes(h[:8], "big") % self.k
        self.assignment[addr] = s
        return s

# ======================================================================

# ======================================================================

class GraphStrategy(Strategy):
    """Deterministic greedy streaming graph placement with capacity constraints.
Repartitions the observed interaction graph at epoch boundaries. Vertices are ordered by degree and scored by neighbor count times remaining capacity. This is a placement baseline, not a reproduction of a full graph-sharding system."""

    name = "Graph"

    def __init__(self, k: int, seed: int = 20260801, repartition_every: int = 1,
                 predictor: str = "persistence"):
        super().__init__(k, seed, predictor=predictor)
        self.repartition_every = repartition_every
        self._adj: dict[str, dict[str, int]] = {}

    def observe(self, a: str, b: str) -> None:
        self._adj.setdefault(a, {}).setdefault(b, 0)
        self._adj.setdefault(b, {}).setdefault(a, 0)
        self._adj[a][b] += 1
        self._adj[b][a] += 1

    def begin_epoch(self, epoch: int, ctx: dict) -> None:
        super().begin_epoch(epoch, ctx)
        if epoch % self.repartition_every == 0 and self._adj:
            self._repartition()

    def _repartition(self) -> None:
        capacity = max(1, int(len(self._adj) * 1.05 / self.k))
        sizes = [0] * self.k
        new_assign: dict[str, int] = {}
        order = sorted(self._adj, key=lambda v: (-len(self._adj[v]), v))
        for v in order:
            best, best_score = 0, float("-inf")
            for i in range(self.k):
                if sizes[i] >= capacity:
                    continue
                nb = sum(w for u, w in self._adj[v].items() if new_assign.get(u) == i)
                score = nb * (1.0 - sizes[i] / capacity)
                if score > best_score:
                    best_score, best = score, i
            new_assign[v] = best
            sizes[best] += 1
        self.assignment = new_assign

    def assign(self, addr: str, tx) -> int:
        if addr in self.assignment:
            return self.assignment[addr]
        s = min(range(self.k), key=lambda i: (self.shards[i].n_tx, i))
        self.assignment[addr] = s
        return s

# ======================================================================

# ======================================================================

class EnhancedStrategy(Strategy):
    """Affinity-aware offline placement with resource weights, load prediction, publication quotas and within-epoch feedback.
Epoch group packing and per-address decision caches determine placement. See docs/IMPLEMENTATION_SCOPE.md for component roles."""

    name = "Enhanced"
    uses_privacy = True

    def __init__(
        self,
        k: int,
        groups: dict[str, int],
        seed: int = 20260801,
        predictor: str = "persistence",
        theta_base: float = 0.2,
        eta: float = 0.1,
        alpha_base: float = 0.3,
        kappa: float = 1.0,
        weight_floor: float = 0.05,
        affinity_rr_epsilon: float = 0.0,
        intra_epoch_feedback: float = 1.5,
        epsilon_base: float = 0.2,
        rebalance_groups: bool = True,
        descending_packing: bool = True,
        intra_epoch_counter: bool = True,
    ):
        super().__init__(k, seed, predictor=predictor)
        self.groups = groups
        self.theta_base = theta_base
        self.eta = eta
        self.alpha_base = alpha_base
        self.kappa = kappa
        self.weight_floor = weight_floor
        self.epsilon_base = epsilon_base

        self.weights = {d: 0.25 for d in DIMS}          # Definition IV.4
        self.lam = [1 / 3, 1 / 3, 1 / 3]                
        self.beta = 0.5                                  # Eq.(19)
        self.budget = PrivacyBudget(epsilon_base_wad=int(epsilon_base * WAD))
        self.group_shard: dict[int, int] = {}
        self.predicted: dict[int, float] = {i: 0.0 for i in range(k)}
        self.pred_err: dict[int, list] = {i: [] for i in range(k)}
        self.beacon = hashlib.sha3_256(str(seed).encode()).digest()
        self.cross_ratio_prev = 0.0
        self.history: list[dict] = []

        # Independent quota for the affinity-margin perturbation; repacking is evaluated separately.
        self.affinity_budget = PrivacyBudget(
            epsilon_base_wad=int(0.8 * WAD), sensitivity_wad=WAD // 2
        )
        self.epoch = 0
        self._epoch_decided: dict[str, int] = {}
        self._epoch_tag = -1

        # Optional randomized response; zero disables it in the paper profile.
        self.affinity_rr_epsilon = affinity_rr_epsilon

        self.group_volume: dict[int, float] = {}
        self._epoch_group_volume: dict[int, int] = {}
        # Track new address assignments within the epoch to respond to stale published loads.
        self._epoch_assigned = [0] * k
        # Within-epoch feedback weight; scripts/reproduce.py fixes the paper profile at 4.5.
        self._intra_weight = intra_epoch_feedback

        # Repack groups at each epoch boundary; compare with the sticky-mapping ablation.
        self.rebalance_groups = rebalance_groups

        self.descending_packing = descending_packing
        if not intra_epoch_counter:
            self._intra_weight = 0.0

        self.pred_used_history: list[dict[int, float]] = []
        self.raw_used_history: list[dict[int, float]] = []

    # ---------------- Algorithm 1 ----------------

    def _adjust_weights(self) -> None:
        g = {d: sum(s.norm[d] for s in self.shards) / len(self.shards) for d in DIMS}
        j = max(DIMS, key=lambda d: g[d])
        w_old = self.weights[j]
        headroom = 1.0 - w_old
        if headroom <= 0:
            return
        dw = min(0.1, headroom)
        self.weights[j] = w_old + dw
        num = headroom - dw
        for d in DIMS:
            if d != j:
                self.weights[d] = self.weights[d] * num / headroom
        
        deficit = 0.0
        for d in DIMS:
            if d != j and self.weights[d] < self.weight_floor:
                deficit += self.weight_floor - self.weights[d]
                self.weights[d] = self.weight_floor
        self.weights[j] -= deficit
        total = sum(self.weights.values())
        self.weights[j] += 1.0 - total          

    def _weighted_load(self, norm: dict[str, float]) -> float:
        return sum(norm[d] * self.weights[d] for d in DIMS)

    # ---------------- Eq.(7) ----------------

    def _alpha(self, shard: int) -> float:
        errs = self.pred_err[shard]
        if not errs:
            return self.alpha_base
        actuals = [a for _, a in errs]
        e = [abs(p - a) for p, a in errs]
        mean_actual = sum(actuals) / len(actuals)
        if mean_actual <= 0:
            return self.alpha_base
        rmse = (sum(x * x for x in e) / len(e)) ** 0.5
        nrmse = rmse / mean_actual
        mean_abs = sum(e) / len(e)
        var = max(0.0, sum(x * x for x in e) / len(e) - mean_abs**2)
        decay = pow(2.718281828459045, -self.kappa * var)
        factor = decay if nrmse == 0 else min(1.0, decay / nrmse)
        return self.alpha_base * factor

    # ---------------- epoch ----------------

    def begin_epoch(self, epoch: int, ctx: dict) -> None:
        super().begin_epoch(epoch, ctx)
        self.epoch = epoch
        self.beacon = hashlib.sha3_256(self.beacon + epoch.to_bytes(8, "big")).digest()
        # Track new address assignments within the epoch to respond to stale published loads.
        self._epoch_decided = {}
        self._epoch_tag = epoch
        self._epoch_assigned = [0] * self.k
        self._rebalance_groups()
        self._epoch_group_volume = {}

    def end_epoch(self, epoch: int, model: ResourceModel, ctx: dict) -> None:
        
        for g, v in self._epoch_group_volume.items():
            prev = self.group_volume.get(g, 0.0)
            self.group_volume[g] = 0.5 * prev + 0.5 * v
        for g in self.group_volume:
            if g not in self._epoch_group_volume:
                self.group_volume[g] *= 0.5

        # Eq.(3)(4)
        pred_used: dict[int, float] = {}
        raw_used: dict[int, float] = {}
        for s in self.shards:
            usage = model.epoch_usage(s.n_tx, s.n_cross, len(s.active), len(s.resident),
                                      epoch=epoch, shard_id=s.shard_id)
            s.norm = model.normalize(usage)
            raw_load = self._weighted_load(s.norm)
            raw_used[s.shard_id] = raw_load
            
            a = self._alpha(s.shard_id)
            pred = self.predicted[s.shard_id]
            pred_used[s.shard_id] = pred
            self.pred_err[s.shard_id].append((pred, raw_load))
            s.load = (1 - a) * raw_load + a * pred
            self.predicted[s.shard_id] = self._advance_predictor(s.shard_id, raw_load)
        self.pred_used_history.append(pred_used)
        self.raw_used_history.append(raw_used)

        # Algorithm 1
        self._adjust_weights()

        for s in self.shards:
            pub = self.budget.publish(
                s.shard_id, int(s.load * WAD), epoch, self.beacon
            )
            s.published_load = pub / WAD

        self._controller_step(ctx)

        self.history.append({
            "epoch": epoch,
            "weights": dict(self.weights),
            "lambda": list(self.lam),
            "beta": self.beta,
            "published": [s.published_load for s in self.shards],
            "true_load": [s.load for s in self.shards],
        })

    # ---------------- Algorithm 2 ----------------

    def _controller_step(self, ctx: dict) -> None:
        loads = [s.published_load for s in self.shards]
        lmax, lmin = max(loads), min(loads)
        lavg = sum(loads) / len(loads) if loads else 0.0
        f1 = min(1.0, ctx.get("cross_ratio", self.cross_ratio_prev))
        f2 = min(1.0, (lmax - lmin) / lavg) if lavg > 0 else 0.0
        f3 = min(1.0, 1.0 - self.budget.average_budget_ratio(list(range(self.k))))
        fbar = (f1 + f2 + f3) / 3
        v = [
            self.lam[0] - self.eta * (fbar - f1),
            self.lam[1] - self.eta * (fbar - f2),
            self.lam[2] - self.eta * (fbar - f3),
        ]
        self.lam = _project_simplex(v)
        denom = self.lam[0] + self.lam[1]
        self.beta = 0.5 if denom == 0 else self.lam[1] / denom
        self.cross_ratio_prev = f1

    # ---------------- Algorithm 3 / 4 ----------------

    def _locality_benefit(self, group_id: int) -> float:
        """Perturb the constant locality margin using the independent affinity quota.
        Relationship leakage is evaluated separately through co-location attacks and repacking ablations.
        """
        raw = 1.0
        eps = self.affinity_budget.adaptive_epsilon(group_id)
        cost = mul_wad(eps, self.affinity_budget.omega)
        if self.affinity_budget._b(group_id) >= cost and eps > 0:
            scale = div_wad(self.affinity_budget.sensitivity, eps)
            n = self.affinity_budget.nonce.get(group_id, 0)
            noise = laplace(self.beacon, PURPOSE_AFFINITY, group_id, n, scale) / WAD
            self.affinity_budget.nonce[group_id] = n + 1
            self.affinity_budget.budget[group_id] = self.affinity_budget._b(group_id) - cost
            val = raw + noise
            self.affinity_budget.last_published[group_id] = int(val * WAD)
            return val
        return self.affinity_budget.last_published.get(group_id, int(raw * WAD)) / WAD

    def _rebalance_groups(self) -> None:
        """Pack groups by estimated volume onto the currently lightest shard.
The default is descending volume; the ablation reverses that order. Sticky mapping can be selected by disabling epoch repacking."""
        if not self.group_volume:
            return
        if not self.rebalance_groups and self.group_shard:
            return
        if self.descending_packing:
            order = sorted(self.group_volume.items(), key=lambda kv: (-kv[1], kv[0]))
        else:
            
            order = sorted(self.group_volume.items(), key=lambda kv: (kv[1], kv[0]))
        bins = [0.0] * self.k
        new_map: dict[int, int] = {}
        for g, v in order:
            i = min(range(self.k), key=lambda j: (bins[j], j))
            new_map[g] = i
            bins[i] += v
        self.group_shard = new_map

    def _initial_shard_for_group(self, g: int, loads: list[float]) -> int:
        """Use a deterministic group hash to distribute initial placements when all published shard loads are tied."""
        lo = min(loads)
        tied = [i for i, v in enumerate(loads) if abs(v - lo) < 1e-12]
        if len(tied) == 1:
            return tied[0]
        h = int.from_bytes(hashlib.sha3_256(f"{self.seed}:group:{g}".encode()).digest()[:8], "big")
        return tied[h % len(tied)]

    def assign(self, addr: str, tx) -> int:
        
        if addr in self._epoch_decided:
            return self._epoch_decided[addr]

        g = self.groups.get(addr, 0)
        
        n_assigned = sum(self._epoch_assigned) or 1
        avg_share = n_assigned / self.k
        loads = [
            sh.published_load
            + self._intra_weight * (self._epoch_assigned[i] - avg_share) / max(1.0, n_assigned)
            for i, sh in enumerate(self.shards)
        ]
        lmax, lmin = max(loads), min(loads)

        chosen = None
        follow = True
        if g != 0 and self.affinity_rr_epsilon > 0:
            import math as _m
            e = _m.exp(self.affinity_rr_epsilon)
            p_follow = e / (e + self.k - 1)
            h = int.from_bytes(
                hashlib.sha3_256(self.beacon + addr.encode()).digest()[:8], "big")
            follow = (h % 10**9) / 10**9 < p_follow

        if g != 0 and follow:
            if g not in self.group_shard:
                self.group_shard[g] = self._initial_shard_for_group(g, loads)
            cand = self.group_shard[g]
            load_diff = 0.0 if lmax == 0 else (lmax - lmin) / lmax
            balance_term = 1.0 - self.beta * load_diff
            privacy_factor = self.budget.average_budget_ratio(list(range(self.k)))
            locality_benefit = self._locality_benefit(g)      
            bracket = (balance_term * self.lam[1] + locality_benefit * self.lam[0]
                       + privacy_factor * self.lam[2])
            threshold = lmin * (1.0 + self.theta_base * bracket) if lmin > 0 else float("inf")
            if loads[cand] <= threshold:
                chosen = cand

        if chosen is None:
            best, best_score = 0, float("inf")
            for i in range(self.k):
                locality = 0.0 if (g != 0 and self.group_shard.get(g) == i) else 1.0
                load_score = loads[i]
                privacy_score = 1.0 - min(1.0, self.budget._b(i) / self.budget.budget_max)
                score = (self.lam[0] * locality + self.lam[1] * load_score
                         + self.lam[2] * privacy_score)
                if score < best_score - 1e-15:
                    best_score, best = score, i
            chosen = best

        self._epoch_decided[addr] = chosen
        self.assignment[addr] = chosen
        self._epoch_assigned[chosen] += 1
        if g != 0:
            self._epoch_group_volume[g] = self._epoch_group_volume.get(g, 0) + 1
        return chosen

def _project_simplex(v: list[float]) -> list[float]:
    """Project the three objective weights onto the ordinary Euclidean simplex."""
    u = sorted(v, reverse=True)
    cum = 0.0
    theta = 0.0
    rho = 0
    for j in range(1, 4):
        cum += u[j - 1]
        if u[j - 1] + (1.0 - cum) / j > 0:
            rho = j
            theta = (cum - 1.0) / j
    out = [max(x - theta, 0.0) for x in v]
    s = sum(out)
    if s <= 0:
        return [1 / 3, 1 / 3, 1 / 3]
    return [x / s for x in out]
