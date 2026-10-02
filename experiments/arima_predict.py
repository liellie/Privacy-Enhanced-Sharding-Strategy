#!/usr/bin/env python3
"""Optional off-chain ARIMA forecast utility.
Uses statsmodels when available; fitting failures use a labeled Holt fallback.
The reproducible paper profile selects Holt directly."""
import argparse, csv, json, sys

def arima_forecast(series, order=(2, 1, 1)):
    try:
        from statsmodels.tsa.arima.model import ARIMA
        m = ARIMA(series, order=order).fit()
        return float(m.forecast(steps=1)[0]), f"ARIMA{order}"
    except Exception as e:
        
        a, b = 0.5, 0.3
        if len(series) < 2:
            return (series[-1] if series else 0.0), "last-value"
        lvl, trd = series[0], series[1] - series[0]
        for x in series[1:]:
            prev = lvl
            lvl = a * x + (1 - a) * (lvl + trd)
            trd = b * (lvl - prev) + (1 - b) * trd
        return float(lvl + trd), f"Holt(fallback: {type(e).__name__})"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--history", required=True,
                    help="CSV，含列 shard_id,epoch,load（万分比 0..10000）")
    ap.add_argument("--order", default="2,1,1")
    ap.add_argument("--out", default="predictions.json")
    args = ap.parse_args()

    order = tuple(int(x) for x in args.order.split(","))
    by_shard = {}
    with open(args.history, encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            by_shard.setdefault(int(row["shard_id"]), []).append(
                (int(row["epoch"]), float(row["load"])))

    out = {}
    for sid, pts in sorted(by_shard.items()):
        series = [v for _, v in sorted(pts)]
        pred, method = arima_forecast(series, order)
        pred = max(0, min(10000, int(round(pred))))
        out[str(sid)] = {"prediction": pred, "method": method, "history_points": len(series)}
        print(f"  shard {sid}: {pred}  ({method}, {len(series)} 点)")

    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=2)
    print(f"\n已写入 {args.out}")
    print("提交上链：ResourceOracle.submit(epoch, shardId, cpu, mem, stor, net, prediction)")

if __name__ == "__main__":
    main()
