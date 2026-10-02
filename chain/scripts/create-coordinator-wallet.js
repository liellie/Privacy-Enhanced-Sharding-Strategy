#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const chainDir = path.resolve(process.argv[2] || path.join(__dirname, ".."));
const ethers = require(path.join(chainDir, "..", "deploy", "node_modules", "ethers"));

const wallet = ethers.Wallet.createRandom();
const privateKey = wallet.privateKey;
const address = wallet.address;
const balance = "0x200000000000000000000000000000000000000000000000000000000000000";

const candidatePaths = [
  path.join(chainDir, "genesis.json"),
  path.join(chainDir, "networkFiles", "genesis.json"),
];
const genesisPath = candidatePaths.find((p) => fs.existsSync(p)) || candidatePaths[0];
const genesis = JSON.parse(fs.readFileSync(genesisPath, "utf8"));
genesis.alloc = genesis.alloc || {};
genesis.alloc[address.slice(2).toLowerCase()] = { balance };
fs.writeFileSync(genesisPath, JSON.stringify(genesis, null, 2) + "\n");

if (!fs.existsSync(path.join(chainDir, "genesis.json"))) {
  fs.writeFileSync(path.join(chainDir, "genesis.json"), JSON.stringify(genesis, null, 2) + "\n");
}

fs.writeFileSync(
	path.join(chainDir, "coordinator.generated.json"),
	JSON.stringify({ address, privateKey }, null, 2) + "\n",
	{ mode: 0o600 }
);

console.log(`    coordinator -> ${address}`);
console.log("    已将 coordinator 账户加入 genesis.alloc，并暂存到 coordinator.generated.json");