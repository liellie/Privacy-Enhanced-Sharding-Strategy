# Results and experiment mapping

Paths below identify the exact preserved datasets, avoiding dependence on figure numbering in a particular manuscript layout.

| Paper result/topic | Reference file | Runner family |
|---|---|---|
| Headline five-seed comparisons | `results/reference/paper/metrics_repeats.csv` | repeats |
| Single-seed strategy comparison | `results/reference/paper/metrics.csv` | base |
| Resource balance and utilization | `results/reference/paper/resource_dims.csv` | base |
| Load and relationship inference | `results/reference/paper/privacy.csv` | base |
| Load epsilon sweep | `results/reference/paper/epsilon_sweep.csv` | eps |
| Observation-window attacks | `results/reference/paper/temporal_attack.csv` | temporal |
| Background knowledge | `results/reference/paper/background_knowledge.csv` | bg |
| Feedback/packing/repacking ablations | `results/reference/paper/ablation_mechanisms.csv`, `ablation_rebalance.csv`, `packing_w_grid.csv` | ablation, base, packing |
| Selected operating point w=4.5 | `results/reference/paper/frontier_repeats.csv` | frontier |
| Extended historical frontier run | `results/reference/frontier/` | frontier profile |
| Load disturbance and prediction lag | `results/reference/adaptability/` | adaptability profile |
| Simulated Byzantine oracle | `results/reference/paper/byzantine_median.csv` | byz |
| Composite score weight sweep | `results/reference/paper/composite_weight_sweep.csv` | composite |
| Chain latency/gas and per-epoch costs | `results/reference/paper/onchain_costs.csv` | onchain |
| Raw archived chain measurements | `results/onchain-input/` | aggregation input, not a replay of the chain |

The three reference directories correspond to distinct historical runs. Their `provenance.json` files record arguments; do not treat every file as output from one invocation. The paper-profile frontier grid includes w=4.5, even though the single-seed 0-to-3 frontier sweep itself ends at 3. The repeated frontier grid is the source for the larger-w operating-point selection.

The optional plotting utility uses descriptive output filenames. Use the topic-to-data mappings above when locating results.
