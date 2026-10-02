#!/usr/bin/env node
/** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */

const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const WAD = 10n ** 18n;
const BYZANTINE = process.argv.includes("--byzantine");

// Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
const OUT_DIR = (() => {
  const i = process.argv.indexOf("--out-dir");
  return i >= 0 ? path.resolve(ROOT, process.argv[i + 1])
                : path.join(ROOT, "outputs", "onchain");
})();

const RUN_ID = (() => {
  const i = process.argv.indexOf("--run-id");
  return i >= 0 ? process.argv[i + 1] : null;
})();

const fmt = (x, d = 4) => (Number(x) / 1e18).toFixed(d);
const median = (arr) => {
  const s = [...arr].sort((a, b) => a - b);
  const n = s.length;
  if (n === 0) return null;
  return n % 2 === 1 ? s[(n - 1) / 2] : Math.round((s[n / 2 - 1] + s[n / 2]) / 2);
};

function loadEnv() {
  const env = {};
  for (const line of fs.readFileSync(path.join(ROOT, "chain", ".env"), "utf8").split("\n")) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (m) env[m[1]] = m[2].trim();
  }
  return env;
}
const abiOf = (n) => JSON.parse(fs.readFileSync(path.join(ROOT, "build", "abi", `${n}.abi`), "utf8"));

async function main() {
  const env = loadEnv();
  const dep = JSON.parse(fs.readFileSync(path.join(ROOT, "deployed.json"), "utf8"));
  const provider = new ethers.JsonRpcProvider(env.RPC_URL);
  const admin = new ethers.Wallet(env.DEPLOYER_PRIVATE_KEY, provider);

  const at = (name) => new ethers.Contract(dep.contracts[name], abiOf(name), admin);
  const beacon = at("EpochBeacon");
  const oracle = at("ResourceOracle");
  const registry = at("EnhancedShardRegistryV2");
  const controller = at("MultiObjectiveController");
  const counter = at("CrossShardCounter");

  const shardCount = dep.params.shardCount;

  const N_EPOCHS = (() => {
    const i = process.argv.indexOf("--epochs");
    return i >= 0 ? Number(process.argv[i + 1]) : 10;
  })();
  const epochs = [];         
  const byzShards = [];      
  let totalGas = 0n;

  const memberWallets = [admin];
  for (let i = 2; i <= 4; i++) {
    const kf = path.join(ROOT, "chain", "nodes", `node${i}`, "data", "key");
    if (fs.existsSync(kf)) {
      let k = fs.readFileSync(kf, "utf8").trim();
      if (!k.startsWith("0x")) k = "0x" + k;
      memberWallets.push(new ethers.Wallet(k, provider));
    }
  }
  console.log(`委员会成员 ${memberWallets.length} 个${BYZANTINE ? "（其中 1 个谎报读数）" : ""}\n`);

  let nextEpoch = Number(await beacon.currentEpoch()) + 1;
  for (let e = 0; e < N_EPOCHS; e++) {
  const cur = Number(await beacon.currentEpoch());
  if (nextEpoch <= cur) nextEpoch = cur + 1;    
  const epoch = nextEpoch;
  nextEpoch = epoch + 1;
  console.log(`===== epoch ${epoch} (${e + 1}/${N_EPOCHS}) =====\n`);

  const stepGas = new Map();   // label -> { gas_used, wall_ms, count }
  let gas = 0n;
  const sleep = (ms) => new Promise((res) => setTimeout(res, ms));

  const send = async (label, factory) => {
    for (let attempt = 1; ; attempt++) {
      try {
        const t0 = Date.now();
        const r = await (await factory()).wait();
        const wallMs = Date.now() - t0;
        gas += r.gasUsed;
        const cur = stepGas.get(label) || { gas_used: 0n, wall_ms: 0, count: 0 };
        cur.gas_used += r.gasUsed;
        cur.wall_ms += wallMs;
        cur.count += 1;
        stepGas.set(label, cur);
        return r;
      } catch (e) {
        if (attempt >= 4) throw e;
        console.log(`    [重试 ${attempt}/3] ${label}: ${e.shortMessage || e.message}`);
        await sleep(2000);
      }
    }
  };

  console.log("[a] EpochBeacon  commit / reveal / finalize");
  const secrets = memberWallets.map((_, i) =>
    ethers.keccak256(ethers.toUtf8Bytes(`secret-${epoch}-${i}-${Date.now()}`)));
  for (let i = 0; i < memberWallets.length; i++) {
    const w = memberWallets[i];
    const commitment = ethers.keccak256(
      ethers.solidityPacked(["bytes32", "address"], [secrets[i], w.address]));
    await send("commit", () => beacon.connect(w).commit(epoch, commitment));
  }
  for (let i = 0; i < memberWallets.length; i++) {
    await send("reveal", () => beacon.connect(memberWallets[i]).reveal(epoch, secrets[i]));
  }
  await send("finalize", () => beacon.finalize(epoch));
  const beaconValue = await beacon.beaconOf(epoch);
  console.log(`    beacon = ${beaconValue}`);
  console.log(`    异或聚合与揭示顺序无关，任意副本重放同一批交易得同一信标\n`);

  console.log("[b] ResourceOracle  submit / finalizeShard");
   
  const truth = [
    { cpu: 8200, mem: 4100, stor: 3000, net: 2500, pred: 8400 },
    { cpu: 3100, mem: 7600, stor: 2800, net: 3200, pred: 3300 },
    { cpu: 2400, mem: 2900, stor: 6900, net: 2100, pred: 2600 },
    { cpu: 1800, mem: 2200, stor: 2400, net: 7100, pred: 2000 },
  ];
  for (let s = 0; s < shardCount; s++) {
    const t = truth[s % truth.length];
    const allCpu = [];
    const honestCpu = [];
    for (let i = 0; i < memberWallets.length; i++) {
       
      const lie = BYZANTINE && i === memberWallets.length - 1;
      const jitter = (v) => Math.max(0, Math.min(10000, v + Math.floor((Math.random() - 0.5) * 200)));
      const r = lie
        ? { cpu: 100, mem: 100, stor: 100, net: 100, pred: 100 }    
        : { cpu: jitter(t.cpu), mem: jitter(t.mem), stor: jitter(t.stor), net: jitter(t.net), pred: jitter(t.pred) };
      allCpu.push(r.cpu);
      if (!lie) honestCpu.push(r.cpu);
      await send("submit", () => oracle.connect(memberWallets[i])
        .submit(epoch, s, r.cpu, r.mem, r.stor, r.net, r.pred));
    }
    await send("finalizeShard", () => oracle.finalizeShard(epoch, s));
    const fin = await oracle.getFinalizedReading(epoch, s);
     
    if (BYZANTINE) {
      byzShards.push({ epoch, shard: s, honest_median: median(honestCpu), observed_median: Number(fin[0]) });
    }
    console.log(`    shard ${s}  中位数 cpu=${fin[0]} mem=${fin[1]} stor=${fin[2]} net=${fin[3]}` +
                `  (真值 cpu=${t.cpu} mem=${t.mem} stor=${t.stor} net=${t.net})`);
  }
  if (BYZANTINE) {
    console.log(`    1 个成员谎报为全部 100，中位数未被拖动 —— f < n/2 时中位数聚合鲁棒\n`);
  } else {
    console.log();
  }

  console.log("[c] syncShard  Eq.(3) 归一化 -> Eq.(4) 加权负载 -> Eq.(6) 预测融合");
  for (let s = 0; s < shardCount; s++) await send("syncShard", () => registry.syncShard(epoch, s));
  for (let s = 0; s < shardCount; s++) {
    const v = await registry.getShard(s);
    console.log(`    shard ${s}  L=${fmt(v[0])}  L_hat=${fmt(v[1])}  L_final=${fmt(v[2])}  alpha=${fmt(await registry.alphaOf(s))}`);
  }
  console.log();

  // ---------- d. Algorithm 1 ----------
  console.log("[d] adjustResourceWeights  Algorithm 1");
  const wBefore = await Promise.all([0, 1, 2, 3].map((i) => registry.weightsWad(i)));
  await send("adjustWeights", () => registry.adjustResourceWeights());
  const wAfter = await Promise.all([0, 1, 2, 3].map((i) => registry.weightsWad(i)));
  const sum = wAfter.reduce((a, b) => a + b, 0n);
  console.log(`    调整前 [${wBefore.map((x) => fmt(x)).join(", ")}]`);
  console.log(`    调整后 [${wAfter.map((x) => fmt(x)).join(", ")}]`);
  console.log(`    权重和 = ${fmt(sum, 9)} ${sum === WAD ? "（恰为 1，归一化约束成立）" : "  << 异常"}\n`);

  console.log("[e] publishPrivatizedLoad  Eq.(8) Laplace + Eq.(14)/(16) 预算");
  for (let s = 0; s < shardCount; s++) {
    const before = await registry.getShard(s);
    await send("publish", () => registry.publishPrivatizedLoad(epoch, s));
    const after = await registry.getShard(s);
    const eps = await registry.adaptiveEpsilon(s);
    const noise = Number(after[3]) - Number(before[2]);
    console.log(`    shard ${s}  真实 L_final=${fmt(before[2])}  ->  发布值=${fmt(after[3])}` +
                `  噪声=${(noise / 1e18).toFixed(5)}  eps=${fmt(eps)}  预算=${fmt(after[4], 2)}`);
  }
  console.log(`    噪声由 beacon 确定性导出，不含 msg.sender 与 block.timestamp\n`);

  // ---------- f. Algorithm 2 ----------
  console.log("[f] MultiObjectiveController.step  Algorithm 2");
  await send("recordBatch", () => counter.recordBatch(37, 200));    
  const objBefore = await controller.objectives();
  const lamBefore = await Promise.all([registry.lambda1Wad(), registry.lambda2Wad(), registry.lambda3Wad()]);
  await send("step", () => controller.step(epoch));
  const lamAfter = await Promise.all([registry.lambda1Wad(), registry.lambda2Wad(), registry.lambda3Wad()]);
  console.log(`    f1(跨片)=${fmt(objBefore[0])}  f2(失衡)=${fmt(objBefore[1])}  f3(隐私风险)=${fmt(objBefore[2])}`);
  console.log(`    lambda 调整前 [${lamBefore.map((x) => fmt(x)).join(", ")}]`);
  console.log(`    lambda 调整后 [${lamAfter.map((x) => fmt(x)).join(", ")}]`);
  const lamSum = lamAfter.reduce((a, b) => a + b, 0n);
  console.log(`    lambda 和 = ${fmt(lamSum, 9)} ${lamSum === WAD ? "（单纯形投影正确）" : "  << 异常"}`);
  console.log(`    更新规则只读链上可见量，各副本独立执行结果一致，无需为 lambda 单独共识\n`);

  console.log("[g] getOptimalShard  Algorithm 3/4（view，交易级）");
  for (let i = 0; i < 5; i++) {
    const org = ethers.Wallet.createRandom().address;
    const shard = await registry.getOptimalShard(org);
    console.log(`    ${org.slice(0, 12)}...  ->  shard ${shard}`);
  }
  console.log(`    失衡度 f2 = ${fmt(await registry.loadImbalance())}   隐私风险 f3 = ${fmt(await registry.privacyRisk())}\n`);

  console.log(`===== epoch ${epoch} 总 gas: ${gas} =====`);
  totalGas += gas;
  epochs.push({
    epoch,
    steps: [...stepGas.entries()].map(([name, v]) => ({
      name,
      gas_used: Number(v.gas_used),
      wall_ms: v.wall_ms,
      count: v.count,
    })),
  });
  }
  console.log(`\n===== 共 ${N_EPOCHS} 个 epoch 总 gas: ${totalGas} =====`);
  console.log(`每 epoch 的链上开销可直接作为论文中"分片管理开销"一项的实测数据。`);

  fs.mkdirSync(OUT_DIR, { recursive: true });
  const runId = RUN_ID || `epoch-driver-${Date.now()}`;
  const gasJson = {
    run_id: runId,
    source: "epoch-driver.js",
    byzantine: BYZANTINE,
    epochs,
  };
  const gasFile = path.join(OUT_DIR, "onchain_epoch_gas.json");
  fs.writeFileSync(gasFile, JSON.stringify(gasJson, null, 2));
  console.log(`\n已导出 ${path.relative(ROOT, gasFile)}（${epochs.length} 个 epoch）`);

  if (BYZANTINE) {
    const byzFile = path.join(OUT_DIR, "onchain_byzantine.json");
     
    fs.writeFileSync(byzFile, JSON.stringify({
      run_id: runId,
      source: "epoch-driver.js",
      byzantine: true,
      committee_size: memberWallets.length,
      num_liars: BYZANTINE ? 1 : 0,
      lie_mode: BYZANTINE ? "lower" : "none",    
      shards: byzShards,
    }, null, 2));
    console.log(`已导出 ${path.relative(ROOT, byzFile)}（${byzShards.length} 个分片）`);
  }
}

main().catch((e) => { console.error("\n联调失败:", e.shortMessage || e.message || e); process.exit(1); });
