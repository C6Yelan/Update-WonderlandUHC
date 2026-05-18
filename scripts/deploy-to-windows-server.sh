#!/usr/bin/env bash
# [常用] 在 WSL 封裝 WonderlandUHC，並部署 jar 到 Windows 1.16.5 測試服。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_DIR="${PLUGIN_DIR:-$ROOT_DIR}"
DEFAULT_WINDOWS_SERVER_DIR="/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.16.5"

WINDOWS_SERVER_DIR="${WINDOWS_SERVER_DIR:-$DEFAULT_WINDOWS_SERVER_DIR}"
TARGET_JAR_NAME="${TARGET_JAR_NAME:-WonderlandUHC.jar}"
WINDOWS_SERVER_PORT="${WINDOWS_SERVER_PORT:-25566}"
SKIP_BUILD=0
FULL_BUILD=0
SKIP_TESTS=0
SKIP_SERVER_CHECK=0

log() {
  printf '[deploy] %s\n' "$*"
}

show_usage() {
  cat <<'EOF'
Usage:
  bash scripts/deploy-to-windows-server.sh [options]

Default:
  Run a fast package step, then copy the latest WonderlandUHC jar to the
  Windows Paper 1.16.5 server plugins directory. This script does not start,
  stop, or restart the server.

Options:
  --server-dir PATH  Windows server directory mounted under WSL.
  --target-name NAME Destination jar name. Default: WonderlandUHC.jar
  --port PORT       Windows server port used for the stopped-server check.
  --full-build       Run package-plugin.sh with a clean build.
  --skip-build       Reuse the latest existing build/libs/WonderlandUHC-*.jar.
  --skip-tests       Pass --skip-tests to package-plugin.sh.
  --skip-server-check
                    Do not check whether the Windows server port is in use.
  -h, --help         Show this help.

Environment:
  PLUGIN_DIR         Plugin repository directory. Default: this repository.
  WINDOWS_SERVER_DIR Override the target Windows server directory.
  TARGET_JAR_NAME    Override the destination plugin jar name.
  WINDOWS_SERVER_PORT
                     Override the Windows server port check. Default: 25566.
EOF
}

latest_plugin_jar() {
  ls -1t "$PLUGIN_DIR"/build/libs/WonderlandUHC-*.jar 2>/dev/null | head -n1
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --server-dir)
        WINDOWS_SERVER_DIR="${2:-}"
        if [[ -z "$WINDOWS_SERVER_DIR" ]]; then
          echo "--server-dir requires a path" >&2
          exit 1
        fi
        shift
        ;;
      --target-name)
        TARGET_JAR_NAME="${2:-}"
        if [[ -z "$TARGET_JAR_NAME" ]]; then
          echo "--target-name requires a file name" >&2
          exit 1
        fi
        shift
        ;;
      --port)
        WINDOWS_SERVER_PORT="${2:-}"
        if [[ -z "$WINDOWS_SERVER_PORT" ]]; then
          echo "--port requires a port number" >&2
          exit 1
        fi
        shift
        ;;
      --full-build)
        FULL_BUILD=1
        ;;
      --skip-build)
        SKIP_BUILD=1
        ;;
      --skip-tests)
        SKIP_TESTS=1
        ;;
      --skip-server-check)
        SKIP_SERVER_CHECK=1
        ;;
      -h|--help)
        show_usage
        exit 0
        ;;
      *)
        echo "Unknown option: $1" >&2
        show_usage
        exit 1
        ;;
    esac
    shift
  done
}

check_windows_server_stopped() {
  if [[ "$SKIP_SERVER_CHECK" == "1" ]]; then
    return 0
  fi

  if ! command -v powershell.exe >/dev/null 2>&1; then
    return 0
  fi

  set +e
  powershell.exe -NoProfile -Command "\$c = Get-NetTCPConnection -LocalPort $WINDOWS_SERVER_PORT -State Listen -ErrorAction SilentlyContinue; if (\$c) { exit 10 }" >/dev/null 2>&1
  local status=$?
  set -e

  if [[ "$status" == "0" ]]; then
    return 0
  fi

  if [[ "$status" == "10" ]]; then
    echo "Windows server port $WINDOWS_SERVER_PORT is currently listening." >&2
    echo "Stop the Windows Paper server before deploying, then rerun this script." >&2
    echo "Use --skip-server-check only if you know replacing the jar is safe." >&2
    exit 1
  fi
}

package_plugin() {
  if [[ "$SKIP_BUILD" == "1" ]]; then
    log "Skipping package step."
    return 0
  fi

  local -a package_args=()
  if [[ "$FULL_BUILD" != "1" ]]; then
    package_args+=(--no-clean)
  fi

  if [[ "$SKIP_TESTS" == "1" ]]; then
    package_args+=(--skip-tests)
  fi

  log "Packaging WonderlandUHC."
  "$ROOT_DIR/scripts/package-plugin.sh" "${package_args[@]}"
}

deploy_plugin() {
  local plugin_jar plugins_dir target_jar temp_jar
  plugin_jar="$(latest_plugin_jar)"

  if [[ -z "$plugin_jar" ]]; then
    echo "Cannot find built plugin jar under $PLUGIN_DIR/build/libs" >&2
    exit 1
  fi

  if [[ ! -d "$WINDOWS_SERVER_DIR" ]]; then
    echo "Windows server directory does not exist: $WINDOWS_SERVER_DIR" >&2
    exit 1
  fi

  check_windows_server_stopped

  plugins_dir="$WINDOWS_SERVER_DIR/plugins"
  mkdir -p "$plugins_dir"

  target_jar="$plugins_dir/$TARGET_JAR_NAME"
  temp_jar="$plugins_dir/.${TARGET_JAR_NAME}.tmp"

  log "Deploying $(basename "$plugin_jar") to $target_jar"
  if ! cp -f "$plugin_jar" "$temp_jar"; then
    echo "Failed to copy jar to $temp_jar" >&2
    echo "If the Windows server is running, stop it and retry." >&2
    exit 1
  fi

  if ! mv -f "$temp_jar" "$target_jar"; then
    rm -f "$temp_jar"
    echo "Failed to replace $target_jar" >&2
    echo "If the Windows server is running, stop it and retry." >&2
    exit 1
  fi

  log "Deployment complete. Restart the Windows Paper server manually: $WINDOWS_SERVER_DIR"
}

main() {
  parse_args "$@"
  package_plugin
  deploy_plugin
}

main "$@"
