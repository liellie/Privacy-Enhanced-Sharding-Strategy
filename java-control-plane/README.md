# Optional Java control-plane prototype

This directory preserves the Java 21/Spring Boot multi-module source. It is optional and is **not** the source of the paper's placement-quality, resource-model or privacy-inference figures. Those are reproduced by the Python entry point at the repository root.

| Module | Responsibility |
|---|---|
| system-api | Database entities/services and web3j bindings |
| coordinator | Epoch scheduling and task orchestration |
| shard-node | Node lifecycle and resource sampling |
| agent-node | Workload submission and separate chain experiment entry point |
| system-controller | Experiment lifecycle and reports |

## Build and configuration

Use JDK 21 and Maven. From this directory:

```bash
mvn -DskipTests install
```

The modules retain the source POM configuration. After installing the reactor and configuring the services, use a Java 21 IDE or run a selected service with, for example, `mvn -pl coordinator spring-boot:run`. A multi-module parent is not itself an executable service. No successful Java compile or service start is claimed for this public extraction.

Configuration templates are under each service's `src/main/resources/` and `nacos-config/shardchain-app.yaml.template`. Copy the service template to `application.yml` (agent uses `application.yaml`) and load the shared configuration into your own Nacos instance. The generated real configurations are ignored by Git.

Supply credentials and deployment addresses via environment variables:

- `NACOS_SERVER_ADDR`, `NACOS_USERNAME`, `NACOS_PASSWORD`.
- `MYSQL_HOST`, `MYSQL_PORT`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- `BLOCKCHAIN_RPC_URL`, `BLOCKCHAIN_PRIVATE_KEY` for the identity of each process.
- `CONTRACT_EPOCH_BEACON`, `CONTRACT_RESOURCE_ORACLE`, `CONTRACT_AFFINITY_MANAGER`, `CONTRACT_CROSS_SHARD_COUNTER`, `CONTRACT_REGISTRY`, `CONTRACT_CONTROLLER`, `CONTRACT_VALIDATOR_REGISTRY`, `CONTRACT_COORDINATOR`, `CONTRACT_ASSET_REGISTRY` from your new deployment.

Initialize tables with `system-api/src/main/resources/mysql/schema.sql`; do not assume MyBatis creates the schema. No database dump or real service configuration is included. If testing the agent's classpath file loader, copy `../experiments/data/transactions.txt` into `agent-node/src/main/resources/data/transactions.txt` before building. That local copy is ignored so the release contains only one workload copy.

## Known integration gaps

Normal submit/load paths create simulated receipts, while `submitForExperiment` sends contract calls. The epoch target, per-shard oracle quorum, database-to-chain ID mapping, prediction submission and error/receipt propagation need further integration work. Rebalance tasks do not implement measured asset-state migration. Byzantine JSON fields emitted by the Java reporter are local submitted values, not actual oracle medians.

See [component interfaces](../docs/IMPLEMENTATION_SCOPE.md) for receipt sources, coordination state and service integration. Use the generated web3j wrappers for contract calls. Build and deploy fresh contracts from the root `contracts/` directory with the JS tools; that build is the authoritative source of deployment bytecode.
