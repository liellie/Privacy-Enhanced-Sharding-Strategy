#!/usr/bin/env bash
# Initialize a fresh local QBFT network and generate validator/coordinator keys. Existing network state is rejected.
#
# 01-init-network.sh
#

#

#
set -euo pipefail

if [ -n "${MSYSTEM:-}" ]; then
  docker() { MSYS_NO_PATHCONV=1 command docker "$@"; }
  hostpath() { cygpath -w "$1"; }
else
  hostpath() { printf '%s' "$1"; }
fi

CHAIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BESU_IMAGE="${BESU_IMAGE:?Set BESU_IMAGE to a pinned Besu image tag or digest}"
NODE_COUNT=4

cd "$CHAIN_DIR"

if [ -d "networkFiles" ] || [ -d "nodes" ]; then
  echo "Existing network detected. Use a fresh checkout for a new network; initialization will not delete keys or data." >&2
  exit 1
fi

echo "==> 1/5 用 Besu operator 生成创世块与验证者密钥"
docker run --rm \
  -v "$(hostpath "$CHAIN_DIR"):/data" \
  -w /data \
  --entrypoint besu \
  "$BESU_IMAGE" \
  operator generate-blockchain-config \
    --config-file=/data/config/qbft-config.json \
    --to=/data/networkFiles \
    --private-key-file-name=key

cp -f "$CHAIN_DIR/networkFiles/genesis.json" "$CHAIN_DIR/genesis.json"

if [ "$(id -u)" != "0" ]; then
  docker run --rm -v "$(hostpath "$CHAIN_DIR"):/data" --entrypoint chown "$BESU_IMAGE" \
    -R "$(id -u):$(id -g)" /data/networkFiles 2>/dev/null || \
    sudo chown -R "$(id -u):$(id -g)" networkFiles
fi

echo "==> 2/5 分发密钥到各节点数据目录"
i=1
declare -a VALIDATOR_ADDRS=()
for keydir in networkFiles/keys/*/; do
  addr="$(basename "$keydir")"
  VALIDATOR_ADDRS+=("$addr")
  mkdir -p "nodes/node${i}/data"
  cp "${keydir}key"     "nodes/node${i}/data/key"
  cp "${keydir}key.pub" "nodes/node${i}/data/key.pub"
  echo "$addr" > "nodes/node${i}/address"
  echo "    node${i} -> ${addr}"
  i=$((i+1))
done

if [ "${#VALIDATOR_ADDRS[@]}" -ne "$NODE_COUNT" ]; then
  echo "错误：期望 ${NODE_COUNT} 个验证者，实际生成 ${#VALIDATOR_ADDRS[@]} 个" >&2
  exit 1
fi

if [ ! -d "$CHAIN_DIR/../deploy/node_modules/ethers" ]; then
  echo "错误：找不到 deploy/node_modules/ethers，请先执行：npm --prefix deploy ci" >&2
  exit 1
fi

echo "==> 3/6 生成独立 coordinator 账户"
node "$CHAIN_DIR/scripts/create-coordinator-wallet.js" "$CHAIN_DIR"

echo "==> 4/6 为验证者账户预置余额"

python3 - "$CHAIN_DIR" "${VALIDATOR_ADDRS[@]}" <<'PY'
import json, sys, pathlib
chain_dir = pathlib.Path(sys.argv[1])
addrs = sys.argv[2:]
g = json.loads((chain_dir / "networkFiles" / "genesis.json").read_text())
alloc = g.setdefault("alloc", {})
for a in addrs:
    alloc[a.lower().replace("0x", "")] = {"balance": "0x200000000000000000000000000000000000000000000000000000000000000"}
(chain_dir / "genesis.json").write_text(json.dumps(g, indent=2))
print(f"    已为 {len(addrs)} 个账户预置余额")
PY

echo "==> 5/6 导出部署账户与 coordinator 账户"
DEPLOYER_ADDR="$(cat nodes/node1/address)"
DEPLOYER_KEY="$(cat nodes/node1/data/key)"
DEPLOYER_KEY="${DEPLOYER_KEY#0x}"
COORDINATOR_ADDR="$(node -e "const x=require('./coordinator.generated.json'); process.stdout.write(x.address)")"
COORDINATOR_KEY="$(node -e "const x=require('./coordinator.generated.json'); process.stdout.write(x.privateKey)")"

cat > .env <<EOF

CHAIN_ID=20260801
RPC_URL=http://localhost:8545
WS_URL=ws://localhost:8546

DEPLOYER_ADDRESS=0x${DEPLOYER_ADDR#0x}
DEPLOYER_PRIVATE_KEY=0x${DEPLOYER_KEY}

COORDINATOR_ADDRESS=${COORDINATOR_ADDR}
COORDINATOR_PRIVATE_KEY=${COORDINATOR_KEY}

$(i=1; for a in "${VALIDATOR_ADDRS[@]}"; do echo "VALIDATOR_${i}=0x${a#0x}"; i=$((i+1)); done)
EOF
chmod 600 .env
rm -f coordinator.generated.json

echo "==> 6/6 完成"
echo
echo "创世块:     ${CHAIN_DIR}/genesis.json"
echo "节点密钥:   ${CHAIN_DIR}/nodes/node{1..4}/data/key"
echo "协调者账户: ${CHAIN_DIR}/deployed.json（部署完成后查看 accounts.coordinator）"
echo "环境变量:   ${CHAIN_DIR}/.env  （含私钥，已设为 600，切勿提交）"
echo
echo "下一步：bash scripts/02-up.sh"
