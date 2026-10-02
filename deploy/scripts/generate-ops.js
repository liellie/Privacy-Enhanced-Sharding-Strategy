#!/usr/bin/env node
/** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */

const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const WAD = 10n ** 18n;

function cliArg(name, def) {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : def;
}

const COUNT = Number(cliArg("--count", 30));
const OUT_DIR = path.resolve(ROOT, cliArg("--out-dir", "outputs/onchain"));
const RUN_ID = cliArg("--run-id", "local-study");
 
const NETEM_DELAY_MS = Number(cliArg("--netem-delay-ms", 50));
const NETEM_JITTER_MS = Number(cliArg("--netem-jitter-ms", 10));
// Implementation and measurement scope: docs/IMPLEMENTATION_SCOPE.md.
const CROSS_RATIO = Number(cliArg("--cross-ratio", 0.5));
 
const PREFLIGHT = process.argv.includes("--preflight");

let _seed = 20260801;
function rand() {
  _seed = (_seed * 1103515245 + 12345) % 2147483648;
  return _seed / 2147483648;
}
function randInt(n) {
  return Math.floor(rand() * n);
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const sendTx = async (factory) => {
  for (let attempt = 1; ; attempt++) {
    try {
      return await (await factory()).wait();
    } catch (e) {
      if (attempt >= 4) throw e;
      console.log(`    [重试 ${attempt}/3] 交易: ${e.shortMessage || e.message}`);
      await sleep(2000);
    }
  }
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
  const provider = new ethers.JsonRpcProvider(env.RPC_URL || dep.rpcUrl);
  const admin = new ethers.Wallet(env.DEPLOYER_PRIVATE_KEY, provider);

  const at = (name) => new ethers.Contract(dep.contracts[name], abiOf(name), admin);
  const beacon = at("EpochBeacon");
  const registry = at("EnhancedShardRegistryV2");
  const validator = at("ValidatorRegistry");
  const assets = at("DigitalAssetRegistry");
  const xshard = at("CrossShardCoordinator");

  const keyMap = {};
  const memberWallets = dep.accounts.shardNodes.map((sn) => {
    const w = new ethers.Wallet(sn.privateKey, provider);
    keyMap[w.address.toLowerCase()] = sn.privateKey;
    return w;
  });

  const poolFile = path.join(ROOT, "deploy", "deployed_shard_addresses.json");
  if (!fs.existsSync(poolFile)) {
    throw new Error(`缺少 ${poolFile}，请先运行 register-shard-addresses.js`);
  }
  const pool = JSON.parse(fs.readFileSync(poolFile, "utf8"));
  const allAddrs = [];
  for (const g of pool.shards) {
    for (const a of g.addresses) allAddrs.push(a);
  }
  console.log(`==> 地址池 ${allAddrs.length} 个（分片 ${pool.shardCount}）`);

  const affinity = new ethers.Contract(
    await registry.affinity(), abiOf("AffinityManager"), admin);
  const bound = [];
  for (const a of allAddrs) {
    const [hasShard, shardId] = await affinity.primaryShardOf(a);
    bound.push({ addr: a, hasShard, shardId: Number(shardId) });
  }
  const unbound = bound.filter((b) => !b.hasShard);
  if (unbound.length > 0) {
    throw new Error(
      `亲和绑定缺失：${unbound.length}/${allAddrs.length} 个地址没有绑定亲和主分片。\n` +
      `    首个：${unbound[0].addr}（groupOf=${await affinity.groupOf(unbound[0].addr)}）\n` +
      `    没有主分片时 getOptimalShard 的打分与地址无关，跨片操作无法产生。\n` +
      `    合约重新部署后亲和组注册会一并失效，请先重跑：\n` +
      `      node deploy/scripts/register-shard-addresses.js`);
  }
  const boundShards = [...new Set(bound.map((b) => b.shardId))];
  console.log(`==> 亲和绑定正常：${allAddrs.length} 个地址覆盖 ${boundShards.length} 个主分片 [${boundShards.join(", ")}]`);

  //

  //
  //         lambda1 / lambda2  >  (L_aff - L_min) / WAD
  //

  const nShards = Number(await registry.shardCount());
  const loads = [];
  for (let i = 0; i < nShards; i++) {
    const info = await registry.getShard(i);
    loads.push({ id: i, published: Number(info[3]) / 1e18, active: info[5] });
  }
  const live = loads.filter((l) => l.active);
  const lmin = Math.min(...live.map((l) => l.published));
  const lmax = Math.max(...live.map((l) => l.published));
  const spread = lmax - lmin;
  const lamRaw = await Promise.all([registry.lambda1Wad(), registry.lambda2Wad()]);
  const lam1 = Number(lamRaw[0]) / 1e18, lam2 = Number(lamRaw[1]) / 1e18;
  const ratio = lam2 === 0 ? Infinity : lam1 / lam2;
  console.log(`==> 分片发布负载 [${live.map((l) => `${l.id}:${l.published.toFixed(4)}`).join(", ")}]，`
    + `展布 ${spread.toFixed(4)}；lambda1/lambda2 = ${ratio.toFixed(4)}`);

  const perShard = live
    .map((l) => `${l.id}: ${(l.published - lmin).toFixed(4)}${l.published - lmin > ratio ? " 越界" : ""}`)
    .join(", ");
  if (ratio <= spread) {

    const needLam1 = (spread * lam2).toFixed(4);
    const needMin = (spread / (1 + 2 * spread)).toFixed(4);
    console.log(
      `    [告警] lambda1/lambda2 = ${ratio.toFixed(4)} 未超过最坏负载展布 ${spread.toFixed(4)}。\n` +
      `           各分片相对最轻分片的超出量：${perShard}\n` +
      `           仅当某地址的主分片标注「越界」时，该地址才会被负载项挤走。\n` +
      `           若下一步的路由干跑失败，两条 admin 调用即可修复（不需重新部署或编译）：\n` +
      `             1) 复位到 lambda = [1/3, 1/3, 1/3]（推荐，比值 1.0，任何展布都过得去）：\n` +
      `                registry.setLambda(WAD/3, WAD/3, WAD - 2*(WAD/3), WAD/2)\n` +
      `             2) 抬高控制器的权重下界，按当前展布需 lambdaMin > ${needMin}，建议取 WAD/4：\n` +
      `                controller.setLambdaMin(WAD/4)\n` +
      `           仅调 lambda1 而不动 lambda2 时，需要 lambda1 >= ${needLam1}（当前 ${lam1.toFixed(4)}）。`);
  }

  const dry = new Map();
  let dryUsable = true;
  try {
    for (const b of bound) {
      const sh = Number(await registry.enhancedLoadBalancing(b.addr));
      if (!dry.has(sh)) dry.set(sh, []);
      dry.get(sh).push(b.addr);
    }
  } catch (e) {
    dryUsable = false;
    console.log(`==> 路由干跑不可用（${e.shortMessage || e.message}），跳过该项，以主循环实测为准`);
  }
  if (dryUsable) {
    const dist = [...dry.entries()].sort((a, b) => a[0] - b[0])
      .map(([sh, a]) => `shard ${sh}: ${a.length}`).join(", ");
    if (dry.size < 2) {
      const affView = bound.map((b) => `${b.addr.slice(0, 8)}\u2192${b.shardId}`).join(" ");
      throw new Error(
        `路由退化（干跑判定）：${allAddrs.length} 个地址全部落于同一分片（${dist}）。\n` +
        `    亲和主分片分布：${affView}\n` +
        `    当前 lambda1/lambda2 = ${ratio.toFixed(4)}，最坏负载展布 ${spread.toFixed(4)}。\n` +
        `    主分片已分散而落点仍收敛于一片，说明局部性折扣敌不过负载项。两条 admin 调用即可修复：\n` +
        `      const WAD = 10n ** 18n;\n` +
        `      await registry.setLambda(WAD/3n, WAD/3n, WAD - 2n*(WAD/3n), WAD/2n);\n` +
        `      await controller.setLambdaMin(WAD/4n);\n` +
        `    改完再跑一次 --preflight 确认，几秒钟出结果，不必等 epoch。\n` +
        `    若主分片本身就集中在一片，则是装箱结果有问题，检查 rebalanceGroups。`);
    }
    console.log(`==> 路由干跑：${allAddrs.length} 个地址落在 ${dry.size} 个分片（${dist}），跨片操作可以产生`);
  }

  if (PREFLIGHT) {
    console.log("\n==> 预检通过（--preflight）。未发起任何交易，未写入任何文件。");
    return;
  }
  console.log("");

  const minStake = await validator.minStake();
  for (const sn of dep.accounts.shardNodes) {
    const v = await validator.validators(sn.address);
    if (v[3]) continue;  // registered
    const w = new ethers.Wallet(sn.privateKey, provider);
    await sendTx(() => validator.connect(w).register({ value: minStake }));
    console.log(`    validator.register ${sn.address} stake=${minStake}`);
  }

  const epochLen = Number(await beacon.epochLength());
  const finalizedEpoch = await ensureFinalized(beacon, memberWallets, provider, epochLen);
  console.log(`==> 当前 epoch ${finalizedEpoch} 已 finalize`);

  await ensureCommittee(validator, finalizedEpoch);
  console.log(`==> 当前 epoch ${finalizedEpoch} 委员会已形成\n`);

  let lastFinalized = finalizedEpoch;
  const ensureEpochReady = async () => {
    const bn = await provider.getBlockNumber();
    const curEpoch = Math.floor(bn / epochLen);
    if (curEpoch !== lastFinalized) {
      console.log(`    epoch 已前进到 ${curEpoch}，重新 finalize + 形成委员会…`);
      lastFinalized = await ensureFinalized(beacon, memberWallets, provider, epochLen);
      await ensureCommittee(validator, lastFinalized);
    }
  };
  const route = async (addr) => Number(await registry.getOptimalShard(addr));

  let groupEpoch = -1;
  let groups = new Map();                 // shard -> [addr]
  const refreshGroups = async () => {
    if (groupEpoch === lastFinalized) return;
    const m = new Map();
    for (const a of allAddrs) {
      const sh = await route(a);
      if (!m.has(sh)) m.set(sh, []);
      m.get(sh).push(a);
    }
    groups = m;
    groupEpoch = lastFinalized;
  };

  const lambdaSnapshot = async () => {
    const l = await Promise.all([
      registry.lambda1Wad(), registry.lambda2Wad(), registry.lambda3Wad(),
    ]);
    return l.map((x) => (Number(x) / 1e18).toFixed(4)).join(", ");
  };

  const degeneracyReport = async () => {
    const nonEmpty = [...groups.values()].filter((g) => g.length > 0).length;
    if (nonEmpty >= 2) return null;
    const sizes = [...groups.entries()].map(([sh, a]) => `shard ${sh}: ${a.length}`).join(", ");
    const affView = bound.map((b) => `${b.addr.slice(0, 8)}→${b.shardId}`).join(" ");
    return (
      `路由退化：${allAddrs.length} 个地址在 epoch ${lastFinalized} 全部落于同一分片（${sizes}）。\n` +
      `    亲和主分片分布：${affView}\n` +
      `    当前 lambda = [${await lambdaSnapshot()}]。\n` +
      `    亲和主分片本身若已分散而路由仍然收敛于一片，说明局部性折扣敌不过负载项，\n` +
      `    两条 admin 调用即可修复（不需重新部署或编译）：\n` +
      `      registry.setLambda(WAD/3, WAD/3, WAD - 2*(WAD/3), WAD/2)\n` +
      `      controller.setLambdaMin(WAD/4)\n` +
      `    若亲和主分片也集中在一片，则是装箱结果本身有问题，检查 rebalanceGroups。`);
  };
  const MAX_DEGENERATE_EPOCHS = 5;
  let degenerateEpochs = 0;

  const pickIntraPair = () => {
    const cands = [...groups.values()].filter((g) => g.length >= 2);
    if (cands.length === 0) return null;
    const g = cands[randInt(cands.length)];
    const i = randInt(g.length);
    let j = randInt(g.length);
    while (j === i) j = randInt(g.length);
    return { sender: g[i], receiver: g[j], senderShard: null, receiverShard: null };
  };

  const pickCrossPair = async () => {
    const keys = [...groups.keys()].filter((sh) => groups.get(sh).length > 0);
    if (keys.length < 2) return null;
    for (let g = 0; g < 20; g++) {
      const i = randInt(keys.length);
      let j = randInt(keys.length);
      while (j === i) j = randInt(keys.length);
      const sa = keys[i], sb = keys[j];
      const cov = await Promise.all(
        [sa, sb].map((sh) => validator.committeeSize(lastFinalized, sh)));
      if (!cov.every((n) => Number(n) > 0)) continue;
      const A = groups.get(sa), B = groups.get(sb);
      return {
        sender: A[randInt(A.length)],
        receiver: B[randInt(B.length)],
        senderShard: sa,
        receiverShard: sb,
        inputShards: [...new Set([sa, sb])],
      };
    }
    return null;
  };

  const waitNextEpoch = async () => {
    const bn = await provider.getBlockNumber();
    const target = Math.floor(bn / epochLen) + 1;
    while (Math.floor(await provider.getBlockNumber() / epochLen) < target) {
      await sleep(2000);
    }
    lastFinalized = await ensureFinalized(beacon, memberWallets, provider, epochLen);
    await ensureCommittee(validator, lastFinalized);
  };

  const operations = [];
  let intra = 0, cross = 0;
  const sha256 = (s) => ethers.sha256(ethers.toUtf8Bytes(s));

  const runNonce = String(Date.now());
  try {
    for (let seq = 1; seq <= COUNT; seq++) {
       
      await ensureEpochReady();

      let sender, receiver, senderShard, receiverShard, inputShards = null, type;
      try {
        await refreshGroups();
        const degen = await degeneracyReport();
        if (degen) {
          degenerateEpochs += 1;
          if (degenerateEpochs > MAX_DEGENERATE_EPOCHS) {
            throw new Error(
              degen + `\n    已连续 ${MAX_DEGENERATE_EPOCHS + 1} 个 epoch 退化，判定为配置问题，中止。`);
          }
          console.log(`    [退化] epoch ${lastFinalized} 全部地址同片，等下一 epoch 重试`
            + `（${degenerateEpochs}/${MAX_DEGENERATE_EPOCHS}）`);
          await waitNextEpoch();
          seq--;
          continue;
        }
        degenerateEpochs = 0;

        const wantCross = cross < (intra + cross + 1) * CROSS_RATIO;
        let pair = wantCross ? await pickCrossPair() : pickIntraPair();
         
        if (!pair) pair = wantCross ? pickIntraPair() : await pickCrossPair();
        if (!pair) {
          await waitNextEpoch();
          seq--;
          continue;
        }

        sender = pair.sender;
        receiver = pair.receiver;
        inputShards = pair.inputShards || null;
         
        senderShard = pair.senderShard !== null && pair.senderShard !== undefined
          ? pair.senderShard : await route(sender);
        receiverShard = pair.receiverShard !== null && pair.receiverShard !== undefined
          ? pair.receiverShard : await route(receiver);
      } catch (e) {
        const msg = e.shortMessage || e.message || "";
        if (/not finalized/i.test(msg)) {
          lastFinalized = await ensureFinalized(beacon, memberWallets, provider, epochLen);
          await ensureCommittee(validator, lastFinalized);
          seq--;
          continue;
        }
        throw e;
      }
      type = senderShard !== receiverShard ? "cross" : "intra";
      if (type === "cross" && !inputShards) {
        inputShards = [...new Set([senderShard, receiverShard])];
      }

      const t0 = Date.now();
      let rec, gasUsed;
      try {
        if (type === "intra") {
          const meta = sha256(`exp-asset-meta:${seq}:${runNonce}`);
          const ref = sha256(`exp-asset-ref:${seq}:${runNonce}`);
          rec = await sendTx(() => assets.createAsset(meta, ref, senderShard));
          gasUsed = rec.gasUsed;
        } else {
          const txId = sha256(`exp-cross-tx:${seq}:${runNonce}`);
          const meta = sha256(`exp-asset-meta:${seq}:${runNonce}`);
          const epoch = lastFinalized;
          gasUsed = 0n;
          gasUsed += (await sendTx(() => xshard.initiate(txId, epoch, inputShards))).gasUsed;
          for (const shard of inputShards) {
            const members = await validator.committeeOf(epoch, shard);
            for (const member of members) {
              const key = keyMap[member.toLowerCase()];
              if (!key) throw new Error(`缺少委员会成员 ${member} 的私钥（shard ${shard}）`);
              const w = new ethers.Wallet(key, provider);
              gasUsed += (await sendTx(() => xshard.connect(w).vote(txId, shard, true, [meta]))).gasUsed;
            }
          }
          rec = await sendTx(() => xshard.commit(txId));
          gasUsed += rec.gasUsed;
        }
      } catch (e) {
        const msg = e.shortMessage || e.message || "";
        if (/not finalized|not formed|committee not formed|not committee|past deadline|empty committee/i.test(msg)) {
           
          lastFinalized = await ensureFinalized(beacon, memberWallets, provider, epochLen);
          await ensureCommittee(validator, lastFinalized);
          seq--;
          continue;
        }
        throw e;
      }
      const t1 = Date.now();
      if (type === "cross") cross++; else intra++;

      operations.push({
        seq,
        type,
        status: "committed",
        submit_ms: t0,
        confirm_ms: t1,
        gas_used: Number(gasUsed),
        tx_hash: rec.hash,
        blockNumber: Number(rec.blockNumber),
        src_shard: senderShard,
        dst_shard: receiverShard,
      });
      if (seq % 5 === 0 || seq === COUNT) {
        console.log(`    进度 ${seq}/${COUNT}  intra=${intra} cross=${cross}`);
      }
    }

  } catch (e) {

    if (operations.length > 0) {
      const partial = writeOps("onchain_ops.partial.json", operations, intra, cross);
      console.error(`\n==> 运行中断，已保存 ${operations.length} 笔已完成操作到 ${partial}`);
    }
    throw e;
  }

  if (intra <= 0 || cross <= 0) {
    const sizes = [...groups.entries()].map(([sh, a]) => `shard ${sh}: ${a.length}`).join(", ");
    throw new Error(
      `路由判定异常：intra=${intra}, cross=${cross}，需两者均 > 0。\n` +
      `    最后一次分组：${sizes}\n` +
      `    lambda = [${await lambdaSnapshot()}]\n` +
      `    cross 恒为 0 且分组只剩一个非空分片时，通常是控制器权重塌缩（lambda1 -> 0）；` +
      `否则检查地址注册与当前 epoch 是否已 finalize。`);
  }

  const out = writeOps("onchain_ops.json", operations, intra, cross);
  console.log(`\n==> 已写入 ${out}`);
  console.log(`    summary: ${JSON.stringify({ overall: operations.length, intra, cross, committed: operations.length, aborted: 0 })}`);
}

function writeOps(fileName, operations, intra, cross) {
  const summary = {
    overall: operations.length,
    intra,
    cross,
    committed: operations.length,
    aborted: 0,
  };
  const t0min = operations.length ? Math.min(...operations.map((o) => o.submit_ms)) : 0;
  const t1max = operations.length ? Math.max(...operations.map((o) => o.confirm_ms)) : 0;
  const actualSpan = Math.max(0.1, (t1max - t0min) / 1000.0);
  const root = {
    run_id: RUN_ID,
    source: "generate-ops.js",
    netem: { enabled: true, delay_ms: NETEM_DELAY_MS, jitter_ms: NETEM_JITTER_MS },
    duration_sec: Number(actualSpan.toFixed(2)),   
    intra_op_count: intra,    
    cross_op_count: cross,    

    sampling: {
      mode: "stratified",
      cross_ratio_target: CROSS_RATIO,
      cross_ratio_actual: Number((cross / Math.max(1, intra + cross)).toFixed(4)),
      note: "intra/cross mix is by design, not a measured cross-shard ratio",
    },
    summary,
    operations,
  };
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const out = path.join(OUT_DIR, fileName);
  fs.writeFileSync(out, JSON.stringify(root, null, 2) + "\n");
  return out;
}

async function ensureCommittee(validator, epoch) {
  if (await validator.committeeFormed(epoch)) return;
  await sendTx(() => validator.formCommittee(epoch));
  console.log(`    formCommittee(${epoch}) 已形成`);
}

async function ensureFinalized(beacon, members, provider, epochLen) {
  for (let attempt = 0; attempt < 60; attempt++) {
    const bn = await provider.getBlockNumber();
    const epoch = Math.floor(bn / epochLen);
    const pos = bn % epochLen;
     
    if (pos > epochLen - 12) {
      if (attempt === 0) console.log(`    正处 epoch ${epoch} 末尾（${pos}/${epochLen}），等待下一 epoch 开始…`);
      await sleep(1500);
      continue;
    }
    try {
      const secrets = members.map((_, i) =>
        ethers.keccak256(ethers.toUtf8Bytes(`genops-${epoch}-${i}-${Date.now()}`)));
      for (let i = 0; i < members.length; i++) {
        const w = members[i];
        const commitment = ethers.keccak256(
          ethers.solidityPacked(["bytes32", "address"], [secrets[i], w.address]));
        await sendTx(() => beacon.connect(w).commit(epoch, commitment));
      }
      for (let i = 0; i < members.length; i++) {
        await sendTx(() => beacon.connect(members[i]).reveal(epoch, secrets[i]));
      }
      await sendTx(() => beacon.finalize(epoch));
      if (Number(await beacon.currentEpoch()) === epoch) {
        return epoch;
      }
      console.log(`    finalize 后 epoch 已前进，重试…`);
    } catch (e) {
      console.log(`    第 ${attempt + 1} 次尝试失败（${e.shortMessage || e.message}），重试…`);
    }
  }
  throw new Error("无法在窗口内 finalize 当前 epoch");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
