#!/usr/bin/env bash
# [常用] 使用 WSL Java 21 執行 Paper 1.21.11 升級線封裝入口。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA21_HOME="${JAVA21_HOME:-/home/ayaya/.jdks/corretto-21}"
LOCAL_GRADLE_HOME="${LOCAL_GRADLE_HOME:-$ROOT_DIR/.gradle-java21-local}"
LOCAL_M2_REPO="${LOCAL_M2_REPO:-$ROOT_DIR/.m2-java21-local}"
CHECK_ENV_ONLY=0

log() {
  printf '[package-1.21] %s\n' "$*"
}

show_usage() {
  cat <<'EOF'
Usage:
  bash scripts/package-plugin-1.21.sh [options passed to package-plugin.sh]
  bash scripts/package-plugin-1.21.sh --check-env

Purpose:
  Run the upgrade-line package entry with WSL Java 21 while keeping Java 8
  baseline caches separate. This is a Step 4 transition script; the stable
  package entry remains scripts/package-plugin.sh until the 1.21.11 build
  platform is fully settled.

Options:
  --check-env       Verify Java 21 and cache paths, then exit without packaging.
  -h, --help        Show this help.

Environment:
  JAVA21_HOME       WSL JDK 21 directory. Default: /home/ayaya/.jdks/corretto-21.
  LOCAL_GRADLE_HOME Java 21 Gradle user home. Default: .gradle-java21-local.
  LOCAL_M2_REPO     Java 21 Maven local repo. Default: .m2-java21-local.

All other options are forwarded to scripts/package-plugin.sh.
EOF
}

java_major_version() {
  "$JAVA21_HOME/bin/java" -version 2>&1 | sed -n 's/.* version "\([0-9][0-9]*\).*/\1/p' | head -n1
}

print_wrapper_version() {
  local wrapper_properties="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"
  if [[ ! -f "$wrapper_properties" ]]; then
    log "Gradle wrapper properties missing."
    return 0
  fi

  local distribution
  distribution="$(sed -n 's/^distributionUrl=.*\/\(gradle-[^/]*\)$/\1/p' "$wrapper_properties")"
  if [[ -n "$distribution" ]]; then
    log "Gradle wrapper distribution: $distribution"
  fi
}

verify_java21() {
  if [[ ! -x "$JAVA21_HOME/bin/java" || ! -x "$JAVA21_HOME/bin/javac" ]]; then
    echo "JAVA21_HOME does not point to an executable JDK 21: $JAVA21_HOME" >&2
    echo "Set JAVA21_HOME=/path/to/jdk-21 and rerun this script." >&2
    exit 1
  fi

  local major
  major="$(java_major_version)"
  if [[ "$major" != "21" ]]; then
    echo "JAVA21_HOME must be JDK 21, but java reports major version: ${major:-unknown}" >&2
    "$JAVA21_HOME/bin/java" -version >&2
    exit 1
  fi
}

parse_args() {
  FORWARDED_ARGS=()
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --check-env)
        CHECK_ENV_ONLY=1
        ;;
      -h|--help)
        show_usage
        exit 0
        ;;
      *)
        FORWARDED_ARGS+=("$1")
        ;;
    esac
    shift
  done
}

main() {
  parse_args "$@"
  verify_java21

  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  export LOCAL_GRADLE_HOME
  export LOCAL_M2_REPO

  mkdir -p "$LOCAL_GRADLE_HOME" "$LOCAL_M2_REPO"

  log "JAVA_HOME=$JAVA_HOME"
  "$JAVA_HOME/bin/java" -version
  "$JAVA_HOME/bin/javac" -version
  log "LOCAL_GRADLE_HOME=$LOCAL_GRADLE_HOME"
  log "LOCAL_M2_REPO=$LOCAL_M2_REPO"
  print_wrapper_version

  if [[ "$CHECK_ENV_ONLY" == "1" ]]; then
    log "Environment check complete; packaging was not run."
    return 0
  fi

  "$ROOT_DIR/scripts/package-plugin.sh" "${FORWARDED_ARGS[@]}"
}

main "$@"
