#!/usr/bin/env bash
# [維護] 清理 WSL 建置與本機快取產生檔。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

safe_cleanup() {
  echo "[cleanup] safe cleanup started"

  rm -rf "$ROOT_DIR/build"
  rm -rf "$ROOT_DIR/.gradle"

  echo "[cleanup] safe cleanup done"
}

deep_cleanup() {
  safe_cleanup

  echo "[cleanup] deep cleanup started"
  rm -rf "$ROOT_DIR/.gradle-local"
  rm -rf "$ROOT_DIR/.m2-local"
  echo "[cleanup] deep cleanup done"
}

show_usage() {
  cat <<'EOF'
Usage:
  bash scripts/clean-workspace.sh          # safe cleanup
  bash scripts/clean-workspace.sh --deep   # deep cleanup
EOF
}

main() {
  case "${1:-}" in
    "")
      safe_cleanup
      ;;
    --deep)
      deep_cleanup
      ;;
    -h|--help)
      show_usage
      ;;
    *)
      echo "Unknown option: $1" >&2
      show_usage
      exit 1
      ;;
  esac
}

main "$@"
