#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const WAD = 10n ** 18n;
const DRY = process.argv.includes("--dry-run");

function loadEnv() {
  const env = {};
  for (const line of fs.readFileSync(path.join(ROOT, "chain", ".env"), "utf8").split("\n")) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (m) env[m[1]] = m[2].trim();
  }
  return env;
}
const abiOf = (n) => JSON.parse(fs.readFileSync(path.join(ROOT, "build", "abi", `${n}.abi`), "utf8"));
const fmt = (x) => (Number(x) / 1e18).toFixed(4);

async function main() {
  const env = loadEnv();
  const dep = JSON.parse(fs.readFileSync(path.join(ROOT, "deployed.json"), "utf8"));
  const provider = new ethers.JsonRpcProvider(env.RPC_URL || dep.rpcUrl);
  const admin = new ethers.Wallet(env.DEPLOYER_PRIVATE_KEY, provider);
  console.log(`==> 调用者 ${admin.address}${DRY ? "（--dry-run，不写入）" : ""}`);

  const registry = new ethers.Contract(
    dep.contracts.EnhancedShardRegistryV2, abiOf("EnhancedShardRegistryV2"), admin);

  const ctrlKey = Object.keys(dep.contracts).find((k) => /MultiObjectiveController/i.test(k));
  const controller = ctrlKey
    ? new ethers.Contract(dep.contracts[ctrlKey], abiOf(ctrlKey), admin)
    : null;

  const before = await Promise.all(
    [registry.lambda1Wad(), registry.lambda2Wad(), registry.lambda3Wad()]);
  console.log(`    当前 lambda = [${before.map(fmt).join(", ")}]`);
  if (controller) {
    console.log(`    当前 lambdaMin = ${fmt(await controller.lambdaMinWad())}（${ctrlKey}）`);
  } else {
    console.log("    [告警] deployed.json 里找不到 MultiObjectiveController，跳过下界设置");
  }

  if (controller) {
    const wired = await registry.controller();
    if (wired.toLowerCase() !== (await controller.getAddress()).toLowerCase()) {
      console.log(`    [告警] registry.controller = ${wired}，与当前控制器 `
        + `${await controller.getAddress()} 不一致。控制器 step() 写不进权重，`
        + `需要 admin 调用 registry.setController(控制器地址)。`);
    }
  }

  const regAdmin = await registry.admin();
  if (regAdmin.toLowerCase() !== admin.address.toLowerCase()) {
    throw new Error(`调用者不是 registry 的 admin（admin = ${regAdmin}），setLambda 会回滚`);
  }
  if (controller) {
    const ctrlAdmin = await controller.admin();
    if (ctrlAdmin.toLowerCase() !== admin.address.toLowerCase()) {
      console.log(`    [告警] 调用者不是控制器的 admin（admin = ${ctrlAdmin}），`
        + "setLambdaMin 会回滚，将跳过该步");
    }
  }

  if (DRY) {
    console.log("\n==> --dry-run 结束，未发起任何交易");
    return;
  }

  const l1 = WAD / 3n, l2 = WAD / 3n, l3 = WAD - l1 - l2;
  await (await registry.setLambda(l1, l2, l3, WAD / 2n)).wait();
  console.log("==> registry.setLambda 已写入");

  if (controller) {
    const ctrlAdmin = await controller.admin();
    if (ctrlAdmin.toLowerCase() === admin.address.toLowerCase()) {
      await (await controller.setLambdaMin(WAD / 4n)).wait();
      console.log("==> controller.setLambdaMin(WAD/4) 已写入");
    }
  }

  const after = await Promise.all(
    [registry.lambda1Wad(), registry.lambda2Wad(), registry.lambda3Wad()]);
  const ratio = Number(after[0]) / Number(after[1]);
  console.log(`==> 复位后 lambda = [${after.map(fmt).join(", ")}]，`
    + `lambda1/lambda2 = ${ratio.toFixed(4)}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
