#!/usr/bin/env node
/**
 * onchain-benchmark.js
 *
 * Measures data unavailable to the offline simulator: receipt gas, block
 * confirmation time, block height, and stateful asset-operation outcomes.
 * The workload and output are deterministic for a fixed sample count and
 * deployment, while timing values come from the live QBFT chain.
 *
 * Outputs:
 *   experiments/onchain_results/onchain_operations.csv
 *   experiments/onchain_results/onchain_summary.csv
 *   experiments/onchain_results/onchain_workload_metrics.csv
 *   experiments/onchain_results/comparison_to_simulation.csv
 *   experiments/onchain_results/analysis.md
 */

const fs = require("fs");
const path = require("path");
const { performance } = require("perf_hooks");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const OUT = path.join(ROOT, "experiments", "onchain_results");
const SAMPLES = Number(process.env.BENCHMARK_SAMPLES || 20);

function loadEnv() {
  const env = {};
  for (const line of fs.readFileSync(path.join(ROOT, "chain", ".env"), "utf8").split("\n")) {
    const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (match) env[match[1]] = match[2].trim();
  }
  return env;
}

function abiOf(name) {
  return JSON.parse(fs.readFileSync(path.join(ROOT, "build", "abi", `${name}.abi`), "utf8"));
}

function csvCell(value) {
  const text = String(value ?? "");
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function csv(rows, columns) {
  return [columns.join(","), ...rows.map((row) => columns.map((c) => csvCell(row[c])).join(","))].join("\n") + "\n";
}

function populationStd(values) {
  if (!values.length) return 0;
  const mean = values.reduce((a, b) => a + b, 0) / values.length;
  return Math.sqrt(values.reduce((sum, value) => sum + (value - mean) ** 2, 0) / values.length);
}

async function main() {
  if (!Number.isInteger(SAMPLES) || SAMPLES < 1) throw new Error("BENCHMARK_SAMPLES must be a positive integer");
  const env = loadEnv();
  const deployed = JSON.parse(fs.readFileSync(path.join(ROOT, "deployed.json"), "utf8"));
  const provider = new ethers.JsonRpcProvider(env.RPC_URL || deployed.rpcUrl || "http://localhost:8545");
  const admin = new ethers.Wallet(env.DEPLOYER_PRIVATE_KEY, provider);
  const asset = new ethers.Contract(deployed.contracts.DigitalAssetRegistry, abiOf("DigitalAssetRegistry"), admin);
  const shardCount = Number(deployed.params.shardCount || 4);
  const nextNonce = new Map();
  nextNonce.set(admin.address.toLowerCase(), await provider.getTransactionCount(admin.address, "pending"));
  const txOptions = (signer) => {
    const address = signer.address.toLowerCase();
    const nonce = nextNonce.get(address);
    if (nonce === undefined) throw new Error(`未初始化 ${signer.address} 的 nonce`);
    nextNonce.set(address, nonce + 1);
    return { nonce };
  };

  const recipients = [];
  for (let i = 1; i <= 4; i++) {
    const keyPath = path.join(ROOT, "chain", "nodes", `node${i}`, "data", "key");
    if (!fs.existsSync(keyPath)) continue;
    let key = fs.readFileSync(keyPath, "utf8").trim();
    if (!key.startsWith("0x")) key = `0x${key}`;
    const wallet = new ethers.Wallet(key, provider);
    if (wallet.address.toLowerCase() !== admin.address.toLowerCase()) {
      recipients.push(wallet);
      nextNonce.set(wallet.address.toLowerCase(), await provider.getTransactionCount(wallet.address, "pending"));
    }
  }
  if (recipients.length === 0) throw new Error("未找到 node1..node4 的验证者账户密钥");

  const rows = [];
  const sendMeasured = async (operation, logicalCrossShard, signer, txPromise, assetId, fromShard, toShard) => {
    const sentAt = performance.now();
    const tx = await txPromise;
    const receipt = await tx.wait();
    const receivedAt = performance.now();
    const block = await provider.getBlock(receipt.blockNumber);
    rows.push({
      sample: rows.length + 1,
      operation,
      logical_cross_shard: logicalCrossShard ? 1 : 0,
      asset_id: assetId,
      from_shard: fromShard,
      to_shard: toShard,
      signer: signer.address,
      tx_hash: receipt.hash,
      block_number: receipt.blockNumber,
      block_timestamp: block ? block.timestamp : "",
      gas_used: receipt.gasUsed.toString(),
      effective_gas_price_wei: receipt.gasPrice ? receipt.gasPrice.toString() : "",
      rpc_confirmation_ms: (receivedAt - sentAt).toFixed(3),
      status: receipt.status === 1 ? "success" : "failed",
    });
    return receipt;
  };

  console.log(`==> 链上资产操作 benchmark: ${SAMPLES} 个样本`);
  console.log(`    RPC=${env.RPC_URL || deployed.rpcUrl || "http://localhost:8545"}`);
  console.log(`    deployer=${admin.address}`);

  for (let i = 0; i < SAMPLES; i++) {
    const initialShard = i % shardCount;
    const destinationShard = (initialShard + 1) % shardCount;
    const metadata = ethers.keccak256(ethers.toUtf8Bytes(`benchmark-metadata-${i}`));
    const encryptedRef = ethers.keccak256(ethers.toUtf8Bytes(`benchmark-ref-${i}`));
    const createSentAt = performance.now();
    const createTx = await asset.createAsset(metadata, encryptedRef, initialShard, txOptions(admin));
    const createReceipt = await createTx.wait();
    const createReceivedAt = performance.now();
    const createdEvent = createReceipt.logs
      .map((log) => { try { return asset.interface.parseLog(log); } catch (_) { return null; } })
      .find((event) => event && event.name === "AssetCreated");
    if (!createdEvent) throw new Error(`样本 ${i} 未找到 AssetCreated 事件`);
    const assetId = createdEvent.args.assetId;
    const createBlock = await provider.getBlock(createReceipt.blockNumber);
    rows.push({
      sample: rows.length + 1,
      operation: "createAsset",
      logical_cross_shard: 0,
      asset_id: assetId.toString(),
      from_shard: "",
      to_shard: initialShard,
      signer: admin.address,
      tx_hash: createReceipt.hash,
      block_number: createReceipt.blockNumber,
      block_timestamp: createBlock ? createBlock.timestamp : "",
      gas_used: createReceipt.gasUsed.toString(),
      effective_gas_price_wei: createReceipt.gasPrice ? createReceipt.gasPrice.toString() : "",
      rpc_confirmation_ms: (createReceivedAt - createSentAt).toFixed(3),
      status: createReceipt.status === 1 ? "success" : "failed",
    });

    const updatedRoot = ethers.keccak256(ethers.toUtf8Bytes(`benchmark-updated-${i}`));
    await sendMeasured("updateMetadata", false, admin,
      asset.updateMetadata(assetId, updatedRoot, encryptedRef, txOptions(admin)), assetId, initialShard, initialShard);

    const recipient = recipients[i % recipients.length];
    await sendMeasured("transferAsset", initialShard !== destinationShard, admin,
      asset.transferAsset(assetId, recipient.address, destinationShard, txOptions(admin)), assetId, initialShard, destinationShard);

    const finalShard = (destinationShard + 1) % shardCount;
    await sendMeasured("transferAsset", destinationShard !== finalShard, recipient,
      asset.connect(recipient).transferAsset(assetId, admin.address, finalShard, txOptions(recipient)), assetId, destinationShard, finalShard);
  }

  const operationColumns = [
    "sample", "operation", "logical_cross_shard", "asset_id", "from_shard", "to_shard",
    "signer", "tx_hash", "block_number", "block_timestamp", "gas_used",
    "effective_gas_price_wei", "rpc_confirmation_ms", "status",
  ];
  const successful = rows.filter((row) => row.status === "success");
  const groups = new Map();
  for (const row of successful) {
    const key = `${row.operation}|${row.logical_cross_shard}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(row);
  }
  const summary = [];
  for (const [key, group] of groups) {
    const [operation, crossShard] = key.split("|");
    const gas = group.map((row) => Number(row.gas_used));
    const latency = group.filter((row) => row.rpc_confirmation_ms !== "").map((row) => Number(row.rpc_confirmation_ms));
    const sortedLatency = [...latency].sort((a, b) => a - b);
    const percentile = (p) => sortedLatency.length ? sortedLatency[Math.min(sortedLatency.length - 1, Math.floor((p / 100) * sortedLatency.length))] : "";
    summary.push({
      operation,
      logical_cross_shard: crossShard,
      samples: group.length,
      success_count: group.length,
      gas_mean: (gas.reduce((a, b) => a + b, 0) / gas.length).toFixed(2),
      gas_min: Math.min(...gas),
      gas_max: Math.max(...gas),
      rpc_confirmation_mean_ms: latency.length ? (latency.reduce((a, b) => a + b, 0) / latency.length).toFixed(3) : "",
      rpc_confirmation_p50_ms: percentile(50),
      rpc_confirmation_p95_ms: percentile(95),
      first_block: group[0].block_number,
      last_block: group[group.length - 1].block_number,
    });
  }
  const summaryColumns = [
    "operation", "logical_cross_shard", "samples", "success_count", "gas_mean", "gas_min", "gas_max",
    "rpc_confirmation_mean_ms", "rpc_confirmation_p50_ms", "rpc_confirmation_p95_ms", "first_block", "last_block",
  ];

  fs.mkdirSync(OUT, { recursive: true });
  fs.writeFileSync(path.join(OUT, "onchain_operations.csv"), csv(rows, operationColumns));
  fs.writeFileSync(path.join(OUT, "onchain_summary.csv"), csv(summary, summaryColumns));

  // Same field names as the simulator, computed only from this chain run.
  // This is a workload-level metric, not a four-strategy replay.
  const shardCounts = Array.from({ length: shardCount }, () => 0);
  for (const row of successful) shardCounts[Number(row.to_shard)] += 1;
  const totalOnchainOps = successful.length;
  const crossOnchainOps = successful.filter((row) => Number(row.logical_cross_shard) === 1).length;
  const shares = shardCounts.map((count) => totalOnchainOps ? count / totalOnchainOps * 100 : 0);
  const loadStdPp = populationStd(shares);
  const meanShare = shares.length ? shares.reduce((a, b) => a + b, 0) / shares.length : 0;
  const loadCvPct = meanShare ? loadStdPp / meanShare * 100 : 0;
  const crossRatioPct = totalOnchainOps ? crossOnchainOps / totalOnchainOps * 100 : 0;
  const localityScore = Math.max(0, 100 - crossRatioPct);
  const balanceScore = Math.max(0, 100 - 10 * loadStdPp);
  const workloadMetrics = [{
    measurement_scope: "onchain_workload_only",
    workload_source: "deterministic benchmark fixture; not experiments/data/transactions.txt",
    strategy: "benchmark_route",
    total_tx: totalOnchainOps,
    cross_shard_ops: crossOnchainOps,
    cross_shard_ratio_pct: crossRatioPct.toFixed(4),
    load_std_pp: loadStdPp.toFixed(4),
    load_cv_pct: loadCvPct.toFixed(4),
    composite_score: (0.4 * balanceScore + 0.6 * localityScore).toFixed(4),
    load_inference_pct: "not_measured",
    relationship_inference_pct: "not_measured",
    resource_dimensions: "not_measured",
    shard_operation_counts: shardCounts.join(";"),
    note: "Comparable field names; not numerically comparable to four-strategy simulation.",
  }];
  const workloadMetricColumns = [
    "measurement_scope", "workload_source", "strategy", "total_tx", "cross_shard_ops",
    "cross_shard_ratio_pct", "load_std_pp", "load_cv_pct", "composite_score",
    "load_inference_pct", "relationship_inference_pct", "resource_dimensions",
    "shard_operation_counts", "note",
  ];
  fs.writeFileSync(path.join(OUT, "onchain_workload_metrics.csv"), csv(workloadMetrics, workloadMetricColumns));

  const simulationMetricsPath = path.join(ROOT, "experiments", "results", "metrics.csv");
  const simulationRows = fs.existsSync(simulationMetricsPath)
    ? parseCsv(fs.readFileSync(simulationMetricsPath, "utf8")) : [];
  const transferSummary = summary.find((row) => row.operation === "transferAsset" && row.logical_cross_shard === "1");
  const comparison = simulationRows.map((row) => ({
    strategy: row.strategy,
    simulation_total_tx: row.total_tx,
    simulation_cross_shard_ratio_pct: row.cross_shard_ratio_pct,
    simulation_load_std_pp: row.load_std_pp,
    simulation_composite_score: row.composite_score,
    onchain_benchmark_samples: rows.length,
    onchain_transfer_gas_mean: transferSummary ? transferSummary.gas_mean : "",
    onchain_transfer_confirmation_mean_ms: transferSummary ? transferSummary.rpc_confirmation_mean_ms : "",
    direct_numeric_comparison: "not_valid: workload and metric definitions differ",
  }));
  const comparisonColumns = [
    "strategy", "simulation_total_tx", "simulation_cross_shard_ratio_pct", "simulation_load_std_pp",
    "simulation_composite_score", "onchain_benchmark_samples", "onchain_transfer_gas_mean",
    "onchain_transfer_confirmation_mean_ms", "direct_numeric_comparison",
  ];
  fs.writeFileSync(path.join(OUT, "comparison_to_simulation.csv"), csv(comparison, comparisonColumns));

  const successCount = rows.filter((row) => row.status === "success").length;
  const elapsedBlocks = Number(rows[rows.length - 1].block_number) - Number(rows[0].block_number) + 1;
  const analysis = [
    "# On-chain experiment analysis",
    "",
    `- Workload: ${SAMPLES} assets, ${rows.length} successful transactions on the live Besu QBFT chain.`,
    `- Success rate: ${successCount}/${rows.length} = ${(successCount / rows.length * 100).toFixed(2)}%.`,
    `- Block span: ${elapsedBlocks} blocks (${rows[0].block_number} to ${rows[rows.length - 1].block_number}).`,
    "- The benchmark measures contract execution cost and RPC confirmation latency.",
    `- Workload-level aligned metrics: ${totalOnchainOps} operations, ${crossOnchainOps} logical cross-shard operations, ` +
      `cross-shard ratio ${crossRatioPct.toFixed(4)}%, load std. ${loadStdPp.toFixed(4)} pp, ` +
      `load CV ${loadCvPct.toFixed(4)}%.`,
    "- These aligned metrics are written to onchain_workload_metrics.csv and use the same formulas as the simulator.",
    "- It does not measure independent-shard parallel throughput or replace the offline strategy comparison.",
    "",
    "## Observed results",
    "",
    ...summary.map((row) => `- ${row.operation} (cross-shard=${row.logical_cross_shard}): ` +
      `${row.samples} samples, mean gas ${row.gas_mean}, ` +
      `mean confirmation ${row.rpc_confirmation_mean_ms || "n/a"} ms, ` +
      `p95 ${row.rpc_confirmation_p95_ms || "n/a"} ms.`),
    "",
    "## Relation to simulation",
    "",
    "The simulation evaluates 47,482 transactions and reports cross-shard ratio, load balance, " +
    "privacy attacks, and a composite score. This on-chain run evaluates 80 contract transactions " +
    "and reports gas and confirmation latency. Therefore no claim of numerical equality is made. " +
    "The comparison CSV preserves both sets of measurements and marks the direct comparison as invalid.",
    "",
    "## Reproduction",
    "",
    "```powershell",
    "$env:BENCHMARK_SAMPLES=\"20\"",
    "npm run benchmark:onchain",
    "```",
  ].join("\n") + "\n";
  fs.writeFileSync(path.join(OUT, "analysis.md"), analysis);
  fs.writeFileSync(path.join(OUT, "config.json"), JSON.stringify({
    rpc: env.RPC_URL || deployed.rpcUrl || "http://localhost:8545",
    chainId: Number((await provider.getNetwork()).chainId),
    samples: SAMPLES,
    shardCount,
    contracts: deployed.contracts,
    generatedAt: new Date().toISOString(),
  }, null, 2) + "\n");
  console.log(`==> 原始数据: experiments/onchain_results/onchain_operations.csv (${rows.length} 笔交易)`);
  console.log(`==> 汇总数据: experiments/onchain_results/onchain_summary.csv`);
  console.log(`==> 同口径指标: experiments/onchain_results/onchain_workload_metrics.csv`);
  console.log(`==> 对照数据: experiments/onchain_results/comparison_to_simulation.csv`);
  console.log(`==> 分析报告: experiments/onchain_results/analysis.md`);
}

function parseCsv(text) {
  const lines = text.trim().split(/\r?\n/);
  if (lines.length < 2) return [];
  const headers = lines[0].split(",");
  return lines.slice(1).map((line) => {
    const values = line.split(",");
    return Object.fromEntries(headers.map((header, i) => [header, values[i] ?? ""]));
  });
}

main().catch((error) => {
  console.error("链上 benchmark 失败:", error.shortMessage || error.message || error);
  process.exit(1);
});
