#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");

const ROOT = path.resolve(__dirname, "../..");
const BUILD = path.join(ROOT, "build");

const WAD = 10n ** 18n;
const EPOCH_LENGTH_BLOCKS = Number(process.env.EPOCH_LENGTH || 30);  
const BEACON_THRESHOLD = Number(process.env.BEACON_THRESHOLD || 3);
const ORACLE_QUORUM = Number(process.env.ORACLE_QUORUM || 3);
const THETA_A = WAD / 2n;           
const ETA = WAD / 10n;              
const SHARD_COUNT = Number(process.env.SHARD_COUNT || 4);
const MIN_STAKE = ethers.parseEther(process.env.MIN_STAKE || "1");
const UNBONDING_EPOCHS = Number(process.env.UNBONDING_EPOCHS || 2);
const XSHARD_TIMEOUT_BLOCKS = Number(process.env.XSHARD_TIMEOUT || 60);
const QUORUM_NUM = 2;     
const QUORUM_DEN = 3;

function loadEnv() {
  const envPath = path.join(ROOT, "chain", ".env");
  if (!fs.existsSync(envPath)) {
    throw new Error("未找到 chain/.env，请先运行 chain/scripts/01-init-network.sh");
  }
  const env = {};
  for (const line of fs.readFileSync(envPath, "utf8").split("\n")) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (m) env[m[1]] = m[2].trim();
  }
  return env;
}

function artifact(name) {
  const abi = JSON.parse(fs.readFileSync(path.join(BUILD, "abi", `${name}.abi`), "utf8"));
  let bin = fs.readFileSync(path.join(BUILD, "bin", `${name}.bin`), "utf8").trim();
  if (!bin.startsWith("0x")) bin = "0x" + bin;
  if (bin.length <= 2) throw new Error(`${name} 字节码为空，请先运行 deploy/compile.sh`);
  return { abi, bin };
}

function readPrivateKey(filePath) {
  return fs.readFileSync(filePath, "utf8").trim();
}

async function deploy(name, wallet, args = []) {
  const { abi, bin } = artifact(name);
  const factory = new ethers.ContractFactory(abi, bin, wallet);
  const c = await factory.deploy(...args);
  const receipt = await c.deploymentTransaction().wait();
  const addr = await c.getAddress();
  console.log(`    ${name.padEnd(28)} ${addr}   gas=${receipt.gasUsed}`);
  return { contract: c, address: addr, gasUsed: receipt.gasUsed };
}

async function main() {
  const env = loadEnv();
  const provider = new ethers.JsonRpcProvider(env.RPC_URL || "http://localhost:8545");
  const wallet = new ethers.Wallet(env.DEPLOYER_PRIVATE_KEY, provider);
  const coordinatorAddress = env.COORDINATOR_ADDRESS || "";
  const shardNodes = [1, 2, 3, 4].map((node) => ({
    node,
    role: "shard-node",
    address: env[`VALIDATOR_${node}`],
    privateKey: readPrivateKey(path.join(ROOT, "chain", "nodes", `node${node}`, "data", "key")),
  }));

  const net = await provider.getNetwork();
  console.log("==> 连接");
  console.log(`    RPC      ${env.RPC_URL}`);
  console.log(`    chainId  ${net.chainId}`);
  console.log(`    部署账户  ${wallet.address}`);
  console.log(`    余额      ${ethers.formatEther(await provider.getBalance(wallet.address))}`);
  console.log();

  console.log("==> 部署");
  let totalGas = 0n;
  const rec = (r) => { totalGas += r.gasUsed; return r; };

  const beacon = rec(await deploy("EpochBeacon", wallet, [EPOCH_LENGTH_BLOCKS, BEACON_THRESHOLD]));
  const oracle = rec(await deploy("ResourceOracle", wallet, [EPOCH_LENGTH_BLOCKS, ORACLE_QUORUM]));
  const affinity = rec(await deploy("AffinityManager", wallet, [THETA_A]));
  const counter = rec(await deploy("CrossShardCounter", wallet, []));
  const registry = rec(await deploy("EnhancedShardRegistryV2", wallet,
    [beacon.address, oracle.address, affinity.address]));
  const controller = rec(await deploy("MultiObjectiveController", wallet,
    [registry.address, counter.address, ETA]));
  const validators = rec(await deploy("ValidatorRegistry", wallet,
    [beacon.address, SHARD_COUNT, MIN_STAKE, UNBONDING_EPOCHS]));
  const xshard = rec(await deploy("CrossShardCoordinator", wallet,
    [validators.address, XSHARD_TIMEOUT_BLOCKS, QUORUM_NUM, QUORUM_DEN]));
  const assetsC = rec(await deploy("DigitalAssetRegistry", wallet, [affinity.address]));
  const mathProbe = rec(await deploy("MathProbe", wallet, []));

  console.log();
  console.log("==> 初始化");
  await (await registry.contract.setController(controller.address)).wait();
  console.log("    registry.setController");

  for (let i = 0; i < SHARD_COUNT; i++) {
    await (await registry.contract.createShard(`shard-${i}`, ethers.ZeroAddress)).wait();
  }
  console.log(`    registry.createShard x${SHARD_COUNT}`);

  await (await affinity.contract.setRecorder(assetsC.address)).wait();
  console.log("    affinity.setRecorder -> DigitalAssetRegistry");

  const members = [env.VALIDATOR_1, env.VALIDATOR_2, env.VALIDATOR_3, env.VALIDATOR_4]
    .filter(Boolean);
  for (const m of members) {
    await (await beacon.contract.addContributor(m)).wait();
    await (await oracle.contract.addMember(m)).wait();
  }
  console.log(`    beacon.addContributor / oracle.addMember x${members.length}`);

  for (const sn of shardNodes) {
    if (!env[`VALIDATOR_${sn.node}`]) continue;
     
    const pk = sn.privateKey.startsWith("0x") ? sn.privateKey : "0x" + sn.privateKey;
    const vWallet = new ethers.Wallet(pk, provider);
    const tx = await validators.contract.connect(vWallet).register({ value: MIN_STAKE });
    const receipt = await tx.wait();
    console.log(
      `    validator.register ${sn.address} stake=${ethers.formatEther(MIN_STAKE)} ETH gas=${receipt.gasUsed}`
    );
  }

  const out = {
    chainId: Number(net.chainId),
    rpcUrl: env.RPC_URL,
    deployer: wallet.address,
    coordinator: coordinatorAddress,
    accounts: {
      coordinator: {
        role: "coordinator",
        address: coordinatorAddress,
        privateKey: env.COORDINATOR_PRIVATE_KEY || "",
        description: "Java coordinator 专用账户，不参与 QBFT 共识",
      },
      shardNodes: shardNodes.map((account) => ({
        ...account,
        description: `Java shard-node-${account.node} 对应 Besu node${account.node} 账户`,
      })),
    },
    deployedAt: new Date().toISOString(),
    params: {
      epochLengthBlocks: EPOCH_LENGTH_BLOCKS,
      beaconThreshold: BEACON_THRESHOLD,
      oracleQuorum: ORACLE_QUORUM,
      thetaAWad: THETA_A.toString(),
      etaWad: ETA.toString(),
      shardCount: SHARD_COUNT,
    },
    contracts: {
      EpochBeacon: beacon.address,
      ResourceOracle: oracle.address,
      AffinityManager: affinity.address,
      CrossShardCounter: counter.address,
      EnhancedShardRegistryV2: registry.address,
      MultiObjectiveController: controller.address,
      ValidatorRegistry: validators.address,
      CrossShardCoordinator: xshard.address,
      DigitalAssetRegistry: assetsC.address,
      MathProbe: mathProbe.address,
    },
    committee: members,
    deploymentGasTotal: totalGas.toString(),
  };
  fs.writeFileSync(path.join(ROOT, "deployed.json"), JSON.stringify(out, null, 2));

  console.log();
  console.log(`==> 部署总 gas: ${totalGas}`);
  console.log(`    地址清单已写入 deployed.json`);
  console.log();
  console.log("下一步：node deploy/scripts/epoch-driver.js  （端到端联调一个 epoch）");
}

main().catch((e) => { console.error("部署失败:", e); process.exit(1); });
