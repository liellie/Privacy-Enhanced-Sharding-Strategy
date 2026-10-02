"""Replay privacy primitives and integer reference transforms.
Load publication uses HMAC-SHA256 and floating-point sampling; affinity margins use Keccak and integer arithmetic. Operational quotas control publication frequency."""

from __future__ import annotations

import hashlib
import hmac
import math

try:
    from Crypto.Hash import keccak as crypto_keccak  # type: ignore
except ModuleNotFoundError:  # pragma: no cover
    crypto_keccak = None

WAD = 10**18
LN2 = 693147180559945309
E_INV = 367879441171442322

PURPOSE_LOAD = 1
PURPOSE_AFFINITY = 2

def mul_wad(a: int, b: int) -> int:
    return (a * b) // WAD

def div_wad(a: int, b: int) -> int:
    if b == 0:
        raise ZeroDivisionError("div_wad by zero")
    return (a * WAD) // b

def isqrt(x: int) -> int:
    if x == 0:
        return 0
    z = (x + 1) // 2
    y = x
    while z < y:
        y = z
        z = (x // z + z) // 2
    return y

def sqrt_wad(x: int) -> int:
    return 0 if x == 0 else isqrt(x * WAD)

def ln_wad(x: int) -> int:
    """Approximate ln(x/WAD)*WAD with range reduction and an odd-power series through z^19."""
    if x <= 0:
        raise ValueError("ln of non-positive")
    if x == WAD:
        return 0
    k = 0
    m = x
    while m < WAD:
        m <<= 1
        k -= 1
    while m >= 2 * WAD:
        m >>= 1
        k += 1
    z = ((m - WAD) * WAD) // (m + WAD)
    z2 = mul_wad(z, z)
    term = z
    s = term
    for d in (3, 5, 7, 9, 11, 13, 15, 17, 19):
        term = mul_wad(term, z2)
        s += term // d
    return 2 * s + k * LN2

def exp_neg_wad(x: int) -> int:
    """e^{-x/WAD}*WAD"""
    if x >= 42 * WAD:
        return 0
    if x == 0:
        return WAD
    n, f = divmod(x, WAD)
    pos, neg = WAD, f
    term = mul_wad(f, f)
    pos += term // 2
    term = mul_wad(term, f); neg += term // 6
    term = mul_wad(term, f); pos += term // 24
    term = mul_wad(term, f); neg += term // 120
    term = mul_wad(term, f); pos += term // 720
    term = mul_wad(term, f); neg += term // 5040
    term = mul_wad(term, f); pos += term // 40320
    r = pos - neg if pos > neg else 0
    for _ in range(n):
        r = mul_wad(r, E_INV)
        if r == 0:
            break
    return r

def _keccak_like(*parts: bytes) -> int:
    """Use Ethereum Keccak-256 with the same packed bytes as Solidity."""
    if crypto_keccak is None:
        raise RuntimeError("缺少 keccak 实现。请安装 pycryptodome: pip install pycryptodome")
    h = crypto_keccak.new(digest_bits=256)
    for p in parts:
        h.update(p)
    return int.from_bytes(h.digest(), "big")

def laplace_from_seed(seed_int: int, scale_wad: int) -> int:
    """Sample the integer inverse Laplace transform using a centered uniform variate and WAD scale."""
    if scale_wad == 0:
        return 0
    u = (seed_int % WAD) - WAD // 2
    negative = u < 0
    abs_u = -u if negative else u
    t = WAD - 2 * abs_u
    if t == 0:
        t = 1
    mag = mul_wad(scale_wad, -ln_wad(t))
    return -mag if negative else mag

def laplace(beacon: bytes, purpose: int, subject: int, nonce: int, scale_wad: int) -> int:
    seed = _keccak_like(
        beacon,
        purpose.to_bytes(1, "big"),
        subject.to_bytes(32, "big"),
        nonce.to_bytes(32, "big"),
    )
    return laplace_from_seed(seed, scale_wad)

def prf_laplace(epoch_seed: bytes, shard_id: int, counter: int, scale: float) -> float:
    """Derive reproducible load noise from HMAC-SHA256(epoch_seed, shard_id|counter).
Map the first eight digest bytes to a floating-point variate and apply the Laplace inverse CDF. Identical inputs reproduce the draw."""
    if scale == 0:
        return 0.0
    h = hmac.new(epoch_seed, f"{shard_id}|{counter}".encode("ascii"), hashlib.sha256).digest()
    u = (int.from_bytes(h[:8], "big") + 1) / (2**64 + 1)  
    v = u - 0.5
    mag = -scale * math.log1p(-2.0 * abs(v))
    return -mag if v < 0 else mag

class PrivacyBudget:
    """Operational quota ledger for load releases.
Default epsilon is 0.2 and load sensitivity is 0.01. Exhaustion reuses the cached release.
Quota recovery is not a reduction of cumulative mathematical privacy loss."""

    def __init__(
        self,
        budget_max_wad: int = 100 * WAD,
        epsilon_base_wad: int = 2 * WAD // 10,
        rho_wad: int = WAD,
        omega_wad: int = WAD,
        gamma_wad: int = WAD // 10,
        sensitivity_wad: int = WAD // 100,
    ):
        self.budget_max = budget_max_wad
        self.epsilon_base = epsilon_base_wad
        self.rho = rho_wad
        self.omega = omega_wad
        self.gamma = gamma_wad
        self.sensitivity = sensitivity_wad
        self.budget: dict[int, int] = {}
        self.last_published: dict[int, int] = {}
        self.last_epoch: dict[int, int] = {}
        self.nonce: dict[int, int] = {}
        self.exhausted_events = 0
        self.publish_events = 0

        self.stale_publish_events = 0

    def _b(self, shard: int) -> int:
        return self.budget.setdefault(shard, self.budget_max)

    def adaptive_epsilon(self, shard: int) -> int:
        loss = max(0, self.budget_max - self._b(shard))
        ratio = div_wad(loss, self.budget_max)
        return div_wad(self.epsilon_base, WAD + mul_wad(self.rho, ratio))

    def publish(self, shard: int, true_load_wad: int, epoch: int, beacon: bytes) -> int:
        
        prev = self.last_epoch.get(shard)
        if prev is not None and epoch > prev + 1:
            elapsed = epoch - prev
            rec = mul_wad(self.gamma, div_wad(WAD, sqrt_wad(elapsed * WAD)))
            self.budget[shard] = min(self.budget_max, self._b(shard) + rec)

        eps = self.adaptive_epsilon(shard)
        cost = mul_wad(eps, self.omega)  # Eq.(14)

        if self._b(shard) >= cost and eps > 0:
            scale = div_wad(self.sensitivity, eps)
            n = self.nonce.get(shard, 0)
            noise = prf_laplace(beacon, shard, n, scale / WAD)
            self.nonce[shard] = n + 1
            published = max(0, true_load_wad + int(noise * WAD))
            self.budget[shard] = self._b(shard) - cost
            self.last_published[shard] = published
            self.publish_events += 1
        else:
            published = self.last_published.get(shard, true_load_wad)
            if shard in self.last_published:
                
                self.stale_publish_events += 1
            self.last_published[shard] = published
            self.exhausted_events += 1

        self.last_epoch[shard] = epoch
        return published

    def average_budget_ratio(self, shards: list[int]) -> float:
        if not shards:
            return 0.0
        return sum(self._b(s) / self.budget_max for s in shards) / len(shards)
