#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const OUT = path.join(ROOT, "deploy", "deployed_shard_addresses.json");
const ADDRS_PER_SHARD = Number(process.env.ADDRS_PER_SHARD || 4);
const SHARD_COUNT = Number(process.env.SHARD_COUNT || 4);
 
const SEED = process.env.REG_SEED || "shardchain-affinity-registration-v1";

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
  const affinityAddr = dep.contracts.AffinityManager;
  const assetsAddr = dep.contracts.DigitalAssetRegistry;
  const affinity = new ethers.Contract(affinityAddr, abiOf("AffinityManager"), admin);

  console.log("==> 组织地址注册");
  console.log(`    AffinityManager   ${affinityAddr}`);
  console.log(`    DigitalAssetRegistry(原 recorder) ${assetsAddr}`);
  console.log(`    每分片地址数 ${ADDRS_PER_SHARD}，分片数 ${SHARD_COUNT}`);
  console.log(`    seed = ${SEED}\n`);

  const hd = ethers.HDNodeWallet.fromSeed(ethers.toUtf8Bytes(SEED));
  const addresses = [];
  for (let i = 0; i < SHARD_COUNT * ADDRS_PER_SHARD; i++) {
    addresses.push(hd.derivePath(`m/44'/60'/0'/0/${i}`).address);
  }

  console.log("[1] 临时切换 recorder -> 部署账户");
  await (await affinity.setRecorder(admin.address)).wait();

  const shards = [];
  try {
    for (let s = 0; s < SHARD_COUNT; s++) {
      const group = addresses.slice(s * ADDRS_PER_SHARD, (s + 1) * ADDRS_PER_SHARD);
      const hub = group[0];
       
      for (let j = 1; j < group.length; j++) {
        await (await affinity.recordInteraction(hub, group[j])).wait();
      }
      const groupId = await affinity.groupOf(hub);
      await (await affinity.setGroupPrimaryShard(groupId, s)).wait();
      shards.push({ shard: s, groupId: Number(groupId), addresses: group });
      console.log(`    分片 ${s}  组 ${Number(groupId)}  <- ${group.map((a) => a.slice(0, 12) + "...").join(", ")}`);
    }
  } finally {
     
    console.log("\n[2] 恢复 recorder -> DigitalAssetRegistry");
    await (await affinity.setRecorder(assetsAddr)).wait();
  }

  console.log("\n[3] 校验 primaryShardOf");
  for (const g of shards) {
    for (const a of g.addresses) {
      const [has, sid] = await affinity.primaryShardOf(a);
      const ok = has && Number(sid) === g.shard;
      console.log(`    ${a.slice(0, 12)}...  primaryShard=${has ? sid : "未绑定"}  ${ok ? "OK" : "<< 异常"}`);
      if (!ok) throw new Error(`地址 ${a} 绑定分片异常`);
    }
  }

  const out = { seed: SEED, shardCount: SHARD_COUNT, addrsPerShard: ADDRS_PER_SHARD, shards };
  fs.writeFileSync(OUT, JSON.stringify(out, null, 2) + "\n");
  console.log(`\n已写入 ${OUT}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
