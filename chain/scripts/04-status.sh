#!/usr/bin/env bash
# Print local QBFT container and RPC status.
#

#
set -euo pipefail

rpc() {
  curl -s -X POST -H 'Content-Type: application/json' \
    --data "{\"jsonrpc\":\"2.0\",\"method\":\"$1\",\"params\":${2:-[]},\"id\":1}" \
    "http://localhost:${3:-8545}"
}

echo "===================== 链状态 ====================="
python3 - <<'PY'
import json, urllib.request

def rpc(method, params=None, port=8545):
    body = json.dumps({"jsonrpc":"2.0","method":method,"params":params or [],"id":1}).encode()
    req = urllib.request.Request(f"http://localhost:{port}",
                                 data=body, headers={"Content-Type":"application/json"})
    try:
        return json.loads(urllib.request.urlopen(req, timeout=5).read()).get("result")
    except Exception as e:
        return None

h = rpc("eth_blockNumber")
print(f"  链 ID:      {int(rpc('eth_chainId') or '0x0', 16)}")
print(f"  当前高度:   {int(h, 16) if h else '不可达'}")

vals = rpc("qbft_getValidatorsByBlockNumber", ["latest"])
if vals:
    print(f"  验证者:     {len(vals)} 个（QBFT 容错 f < n/3，n={len(vals)} 可容忍 {(len(vals)-1)//3} 个拜占庭节点）")
    for v in vals:
        print(f"              {v}")

print("\n  各节点高度:")
for i, port in enumerate([8545, 8547, 8548, 8549], start=1):
    hh = rpc("eth_blockNumber", port=port)
    peers = rpc("net_peerCount", port=port)
    print(f"    node{i} (:{port})  高度={int(hh,16) if hh else '不可达':<8} 对等节点={int(peers,16) if peers else '-'}")

if h and int(h, 16) > 2:
    n = int(h, 16)
    b1 = rpc("eth_getBlockByNumber", [hex(n), False])
    b0 = rpc("eth_getBlockByNumber", [hex(n-1), False])
    if b1 and b0:
        dt = int(b1["timestamp"], 16) - int(b0["timestamp"], 16)
        print(f"\n  最近出块间隔: {dt}s（配置值 2s）")
PY
echo "=================================================="
