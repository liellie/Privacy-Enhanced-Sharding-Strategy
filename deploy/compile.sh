#!/usr/bin/env bash
# Compile Solidity with the pinned compiler and export ABI/BIN artifacts.
set -euo pipefail

if [ -n "${MSYSTEM:-}" ]; then
  docker() { MSYS_NO_PATHCONV=1 command docker "$@"; }
  hostpath() { cygpath -w "$1"; }
else
  hostpath() { printf '%s' "$1"; }
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOLC_IMAGE="${SOLC_IMAGE:-ethereum/solc:0.8.19}"

cd "$ROOT"
rm -rf build/abi build/bin build/combined.json
mkdir -p build/abi build/bin

echo "==> 编译（solc 0.8.19，优化 200 轮）"
docker run --rm \
  -v "$(hostpath "$ROOT/contracts"):/sources:ro" \
  -v "$(hostpath "$ROOT/build"):/output" \
  "$SOLC_IMAGE" \
  --optimize --optimize-runs 200 \
  --combined-json abi,bin \
  --base-path /sources \
  /sources/FixedPointMath.sol \
  /sources/DeterministicLaplace.sol \
  /sources/MathProbe.sol \
  /sources/EpochBeacon.sol \
  /sources/ResourceOracle.sol \
  /sources/AffinityManager.sol \
  /sources/EnhancedShardRegistryV2.sol \
  /sources/MultiObjectiveController.sol \
  /sources/CrossShardCounter.sol \
  /sources/CrossShardMetricsAdapter.sol \
  /sources/ValidatorRegistry.sol \
  /sources/CrossShardCoordinator.sol \
  /sources/DigitalAssetRegistry.sol \
  > build/combined.json

echo "==> 拆分 ABI 与 BIN"
python3 - <<'PY'
import json, pathlib
root = pathlib.Path(".")
data = json.loads((root / "build" / "combined.json").read_text())
count = 0
oversize = []
for full_name, art in data["contracts"].items():
    name = full_name.split(":")[-1]
    abi = art.get("abi")
    if isinstance(abi, str):
        abi = json.loads(abi)
    (root / "build" / "abi" / f"{name}.abi").write_text(json.dumps(abi))
    binhex = art.get("bin", "")
    (root / "build" / "bin" / f"{name}.bin").write_text(binhex)
    size = len(binhex) // 2

    flag = ""
    if size > 24576:
        oversize.append((name, size)); flag = "  << 超出 EIP-170 上限"
    elif size > 20000:
        flag = "  << 接近上限"
    if size > 0:
        print(f"    {name:32s} {size:>6d} 字节{flag}")
    count += 1
print(f"\n    共 {count} 个产物")
if oversize:
    print("\n  以下合约超过 24576 字节部署上限，必须拆分或提高优化轮数：")
    for n, s in oversize:
        print(f"    {n}: {s}")
    raise SystemExit(1)
PY

echo
echo "ABI: build/abi/    BIN: build/bin/"
echo
echo "生成 Java 包装类（使用 web3j CLI）："
echo "  web3j generate solidity \\"
echo "    --abiPath build/abi/EnhancedShardRegistryV2.abi \\"
echo "    --binPath build/bin/EnhancedShardRegistryV2.bin \\"
echo "    --outputDir java-control-plane/system-api/src/main/java \\"
echo "    --package org.example.blockchain.contract"
