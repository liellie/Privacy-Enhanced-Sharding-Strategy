const fs = require("fs");
const { ethers } = require("ethers");
const dep = JSON.parse(fs.readFileSync("../deployed.json"));
const reg = JSON.parse(fs.readFileSync("deployed_shard_addresses.json"));
const env = {};
for (const l of fs.readFileSync("../chain/.env", "utf8").split("\n")) {
  const m = l.match(/^([A-Z0-9_]+)=(.*)$/);
  if (m) env[m[1]] = m[2].trim();
}
const p = new ethers.JsonRpcProvider(env.RPC_URL);
const abi = JSON.parse(fs.readFileSync("../build/abi/EnhancedShardRegistryV2.abi"));
const r = new ethers.Contract(dep.contracts.EnhancedShardRegistryV2, abi, p);
(async () => {
  let ok = 0, tot = 0;
  for (const g of reg.shards) {
    for (const a of g.addresses) {
      const s = await r.getOptimalShard(a);
      tot++;
      const match = Number(s) === g.shard;
      if (match) ok++;
      console.log("期望shard" + g.shard, a.slice(0, 10), "-> shard", Number(s), match ? "OK" : "DIFF");
    }
  }
  console.log("\n" + ok + "/" + tot + " 命中绑定分片");
})();
