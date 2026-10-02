#!/usr/bin/env bash
# Inject network impairment: delay <ms> <jitter>, loss <percent>, or clear.
#

#
# Inject reproducible network impairment between local QBFT nodes.
#

#

set -euo pipefail

PUMBA_IMAGE="${PUMBA_IMAGE:-gaiaadm/pumba:latest}"
TARGET_RE="re2:besu-node"
DURATION="${NETEM_DURATION:-24h}"

run_pumba() {
  docker run --rm -d \
    --name pumba-netem \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "$PUMBA_IMAGE" \
    netem --duration "$DURATION" --tc-image gaiadocker/iproute2 "$@" "$TARGET_RE"
}

case "${1:-}" in
  delay)
    MS="${2:-50}"; JITTER="${3:-10}"
    docker rm -f pumba-netem >/dev/null 2>&1 || true
    run_pumba delay --time "$MS" --jitter "$JITTER" --distribution normal
    echo "已注入 ${MS}ms ±${JITTER}ms 时延，持续 ${DURATION}"
    echo "提示：实验报告中须写明该配置，否则延迟数据无法复现。"
    ;;
  loss)
    PCT="${2:-1}"
    docker rm -f pumba-netem >/dev/null 2>&1 || true
    run_pumba loss --percent "$PCT"
    echo "已注入 ${PCT}% 丢包，持续 ${DURATION}"
    ;;
  clear)
    docker rm -f pumba-netem >/dev/null 2>&1 || true
    echo "已清除网络损伤"
    ;;
  *)
    echo "用法: $0 {delay <ms> [jitter] | loss <percent> | clear}" >&2
    exit 1
    ;;
esac
