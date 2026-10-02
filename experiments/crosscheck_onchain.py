#!/usr/bin/env python3
"""Compare Python fixed-point helpers with deployed MathProbe functions through JSON-RPC.
Requires a running chain, deployed MathProbe, and deployment metadata. Use --help for connection arguments."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.request
from pathlib import Path

try:
    import sha3  # type: ignore
except ModuleNotFoundError:  # pragma: no cover
    sha3 = None

try:
    from Crypto.Hash import keccak as crypto_keccak  # type: ignore
except ModuleNotFoundError:  # pragma: no cover
    crypto_keccak = None

sys.path.insert(0, str(Path(__file__).resolve().parent))

from src.privacy import WAD, ln_wad, exp_neg_wad, laplace

def rpc_call(url: str, method: str, params):
    payload = json.dumps({"jsonrpc": "2.0", "method": method, "params": params, "id": 1}).encode()
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.loads(resp.read().decode())
    if "error" in data:
        raise RuntimeError(f"RPC error: {data['error']}")
    return data["result"]

def keccak256_digest(data: bytes) -> bytes:
    if sha3 is not None:
        h = sha3.keccak_256()
        h.update(data)
        return h.digest()
    if crypto_keccak is not None:
        h = crypto_keccak.new(digest_bits=256)
        h.update(data)
        return h.digest()
    raise RuntimeError("缺少 keccak 实现。请安装 pycryptodome: pip install pycryptodome")

def encode_uint256(value: int) -> bytes:
    return value.to_bytes(32, byteorder="big", signed=False)

def selector_for_sig(sig: str) -> str:
    return keccak256_digest(sig.encode("utf-8")).hex()[:8]

def to_int_hex(x: int) -> str:
    return hex(x) if isinstance(x, int) else str(x)

def call_view_contract(url: str, contract_addr: str, abi: list, fn_name: str, args):
    selector = None
    for item in abi:
        if item.get("name") == fn_name and item.get("type") == "function":
            selector = item
            break
    if selector is None:
        raise RuntimeError(f"ABI 中未找到函数 {fn_name}")
    sig = fn_name + "(" + ",".join(p.get("type", "uint256") for p in selector.get("inputs", [])) + ")"
    selector_hex = selector_for_sig(sig)
    data = "0x" + selector_hex
    for arg in args:
        # encode uint256 / int256 / bytes32 as 32-byte hex
        if isinstance(arg, int):
            h32 = encode_uint256(arg)
            data += h32.hex()
        elif isinstance(arg, str) and arg.startswith("0x"):
            h32 = bytes.fromhex(arg[2:].rjust(64, "0"))
            data += h32.hex()
        elif isinstance(arg, bytes):
            data += arg.hex().rjust(64, "0")
        else:
            raise TypeError(f"Unsupported arg type for encoding: {type(arg)}")

    return rpc_call(url, "eth_call", [{"to": contract_addr, "data": data}, "latest"])

def decode_uint256_hex(result: str) -> int:
    if not result.startswith("0x"):
        raise ValueError(f"Not a hex result: {result!r}")
    return int(result, 16)

def decode_int256_hex(result: str) -> int:
    value = decode_uint256_hex(result)
    # two's complement decode for signed 256-bit
    if value >= 2**255:
        value -= 2**256
    return value

def load_deployed(path: str):
    p = Path(path)
    if not p.exists():
        raise FileNotFoundError(f"未找到 deployed.json: {p}")
    return json.loads(p.read_text(encoding="utf-8"))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--rpc", default="http://localhost:8545")
    ap.add_argument("--deployed", default="../deployed.json")
    ap.add_argument("--probe", default="0x0000000000000000000000000000000000000000", help="MathProbe 合约地址")
    args = ap.parse_args()

    deployed = load_deployed(args.deployed)
    probe_addr = args.probe
    if probe_addr == "0x0000000000000000000000000000000000000000":
        if "contracts" in deployed and "MathProbe" in deployed["contracts"]:
            probe_addr = deployed["contracts"]["MathProbe"]
        elif "MathProbe" in deployed:
            probe_addr = deployed["MathProbe"]
        else:
            raise SystemExit(
                "未提供 --probe，也未在 deployed.json 中找到 MathProbe 地址。\n"
                "请先部署 MathProbe.sol 并把地址写进 deployed.json 或使用 --probe 指定。"
            )

    print(f"==> 对照目标：{args.rpc}")
    print(f"==> MathProbe: {probe_addr}")
    print()

    abi_path = Path(__file__).resolve().parent.parent / "build" / "abi" / "MathProbe.abi"
    if not abi_path.exists():
        raise SystemExit(
            f"未找到 ABI: {abi_path}\n"
            "先运行：bash deploy/compile.sh\n"
            "或者手动编译 MathProbe.sol 生成 ABI。"
        )
    abi = json.loads(abi_path.read_text(encoding="utf-8"))

    cases = [
        ("lnWad", "lnWadPublic", [1_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [100_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [500_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [900_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [1_000_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [1_500_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [2_000_000_000_000_000_000], lambda x: ln_wad(x)),
        ("lnWad", "lnWadPublic", [10_000_000_000_000_000_000], lambda x: ln_wad(x)),
        ("expNegWad", "expNegWadPublic", [0], lambda x: exp_neg_wad(x)),
        ("expNegWad", "expNegWadPublic", [500_000_000_000_000_000], lambda x: exp_neg_wad(x)),
        ("expNegWad", "expNegWadPublic", [1_000_000_000_000_000_000], lambda x: exp_neg_wad(x)),
        ("expNegWad", "expNegWadPublic", [2_000_000_000_000_000_000], lambda x: exp_neg_wad(x)),
        ("expNegWad", "expNegWadPublic", [5_000_000_000_000_000_000], lambda x: exp_neg_wad(x)),
    ]

    print("==> Python 参考值 vs 链上真实值")
    print("    仅比较被包装暴露出来的定点函数，输出 PASS / FAIL")
    print()

    ok_total = 0
    fail_total = 0

    for name, fn_name, params, py_fn in cases:
        py_val = py_fn(params[0])
        data = "0x" + ""  # placeholder
        data = "0x" + ""  # not used

        # exact call directly to chain
        # ABI selector encoding for uint256 args
        sel = None
        for item in abi:
            if item.get("name") == fn_name and item.get("type") == "function":
                sel = item
                break
        if sel is None:
            raise RuntimeError(f"ABI 中没有找到 {fn_name}")

        sig = fn_name + "(" + ",".join(inp.get("type") for inp in sel.get("inputs", [])) + ")"
        selector = selector_for_sig(sig)
        encoded = selector + encode_uint256(params[0]).hex()
        result = rpc_call(args.rpc, "eth_call", [{"to": probe_addr, "data": "0x" + encoded}, "latest"])

        if fn_name == "lnWadPublic":
            chain_val = decode_int256_hex(result)
        else:
            chain_val = decode_uint256_hex(result)
        ok = chain_val == py_val
        status = "PASS" if ok else "FAIL"
        print(f"{name:10s} input={params[0]:>20d}  python={py_val:>20d}  chain={chain_val:>20d}  {status}")
        if ok:
            ok_total += 1
        else:
            fail_total += 1

    print()
    print("==> Laplace 单点对照（固定 seed 的逆变换）")
    seed_cases = [
        (1, 15_000_000_000_000_000),
        (12_345, 15_000_000_000_000_000),
        (2**200, 15_000_000_000_000_000),
        (2**255, 15_000_000_000_000_000),
    ]
    for seed, scale_wad in seed_cases:
        py_val = laplace(seed.to_bytes(32, byteorder="big"), 1, seed, 0, scale_wad)
        # contract uses keccak256(beacon|purpose|subject|nonce) with bytes32 beacon, uint8 purpose, uint256 subject, uint256 nonce
        beacon = seed.to_bytes(32, byteorder="big")
        sig = "laplacePublic(bytes32,uint8,uint256,uint256,uint256)"
        selector = selector_for_sig(sig)
        payload = (
            selector
            + beacon.hex()
            + (1).to_bytes(32, byteorder="big").hex()
            + seed.to_bytes(32, byteorder="big").hex()
            + (0).to_bytes(32, byteorder="big").hex()
            + scale_wad.to_bytes(32, byteorder="big").hex()
        )
        result = rpc_call(args.rpc, "eth_call", [{"to": probe_addr, "data": "0x" + payload}, "latest"])
        chain_val = decode_int256_hex(result)
        ok = chain_val == py_val
        print(f"laplace seed={seed} scale={scale_wad}  python={py_val}  chain={chain_val}  {'PASS' if ok else 'FAIL'}")
        if ok:
            ok_total += 1
        else:
            fail_total += 1

    print()
    print(f"Summary: PASS={ok_total}, FAIL={fail_total}")
    if fail_total:
        print("结论：Python 和链上结果存在差异，需要检查 Solidity 实现、ABI 编码、或 MathProbe 部署方式。")
    else:
        print("结论：Python 与链上结果一致，定点数学实现已通过对照检查。")

if __name__ == "__main__":
    main()
