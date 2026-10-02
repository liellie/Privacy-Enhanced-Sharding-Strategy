#!/usr/bin/env bash
# Start the local QBFT containers using the generated network configuration.
#

#
set -euo pipefail

CHAIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$CHAIN_DIR"

[ -f genesis.json ] || { echo "未找到 genesis.json，请先运行 scripts/01-init-network.sh" >&2; exit 1; }

echo "==> 生成 static-nodes.json"

python3 - <<'PY'
import json, pathlib
ips = {1: "172.28.0.11", 2: "172.28.0.12", 3: "172.28.0.13", 4: "172.28.0.14"}
enodes = []
for i in sorted(ips):
    pub = pathlib.Path(f"nodes/node{i}/data/key.pub").read_text().strip()
    if pub.startswith("0x"):
        pub = pub[2:]
    enodes.append(f"enode://{pub}@{ips[i]}:30303")
pathlib.Path("static-nodes.json").write_text(json.dumps(enodes, indent=2))
for e in enodes:
    print("   ", e[:40] + "..." + e[-22:])
PY

echo "==> 启动容器"
docker compose up -d

echo "==> 等待出块"
for attempt in $(seq 1 40); do
  height="$(curl -s -X POST -H 'Content-Type: application/json' \
      --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
      http://localhost:8545 2>/dev/null | python3 -c \
      'import sys,json; d=json.load(sys.stdin); print(int(d["result"],16))' 2>/dev/null || echo "")"
  if [ -n "$height" ] && [ "$height" -gt 0 ] 2>/dev/null; then
    echo "    已出块，当前高度 ${height}"
    break
  fi
  printf '.'
  sleep 3
done
echo

if [ -z "${height:-}" ] || [ "${height:-0}" -eq 0 ] 2>/dev/null; then
  echo "网络尚未出块。QBFT 需要至少 3 个验证者在线（4 节点容错 f=1）。" >&2
  echo "排查：docker compose logs --tail=80 besu-node1" >&2
  exit 1
fi

bash "$CHAIN_DIR/scripts/04-status.sh"
