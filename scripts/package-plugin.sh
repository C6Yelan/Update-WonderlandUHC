#!/usr/bin/env bash
# [常用] 執行 Gradle 測試，並封裝 WonderlandUHC shadow jar。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_DIR="${PLUGIN_DIR:-$ROOT_DIR}"
LOCAL_GRADLE_HOME="${LOCAL_GRADLE_HOME:-$ROOT_DIR/.gradle-local}"

RUN_TESTS=1
CLEAN_BUILD=1

log() {
  printf '[package] %s\n' "$*"
}

show_usage() {
  cat <<'EOF'
Usage:
  bash scripts/package-plugin.sh [options]

Options:
  --skip-tests       Build the plugin jar without running Gradle tests.
  --no-clean         Do not run the Gradle clean task before packaging.
  -h, --help         Show this help.

Environment:
  PLUGIN_DIR         Plugin repository directory. Default: this repository.
  LOCAL_GRADLE_HOME Gradle user home used by this repository.
  GRADLE_EXTRA_ARGS Extra arguments appended to ./gradlew.
EOF
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
}

ensure_gradle_wrapper_jar() {
  local wrapper_jar="$PLUGIN_DIR/gradle/wrapper/gradle-wrapper.jar"
  if [[ -f "$wrapper_jar" ]]; then
    return 0
  fi

  log "gradle-wrapper.jar missing; downloading Gradle 8.10.2 wrapper jar."
  curl -sSL --fail \
    -o "$wrapper_jar" \
    "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
}

build_plugin() {
  log "Building WonderlandUHC."
  (
    cd "$PLUGIN_DIR"
    local -a gradle_args=()

    if [[ "$CLEAN_BUILD" == "1" ]]; then
      gradle_args+=(clean)
    fi

    if [[ "$RUN_TESTS" == "1" ]]; then
      gradle_args+=(test)
    fi

    gradle_args+=(shadowJar --no-daemon -Dorg.gradle.native=false)

    if [[ -n "${GRADLE_EXTRA_ARGS:-}" ]]; then
      local -a extra_args
      read -r -a extra_args <<<"$GRADLE_EXTRA_ARGS"
      gradle_args+=("${extra_args[@]}")
    fi

    GRADLE_USER_HOME="$LOCAL_GRADLE_HOME" ./gradlew "${gradle_args[@]}"
  )
}

latest_plugin_jar() {
  ls -1t "$PLUGIN_DIR"/build/libs/WonderlandUHC-*.jar 2>/dev/null | head -n1
}

print_result() {
  local plugin_jar
  plugin_jar="$(latest_plugin_jar)"
  if [[ -z "$plugin_jar" ]]; then
    echo "Cannot find built plugin jar under $PLUGIN_DIR/build/libs" >&2
    exit 1
  fi

  log "Built plugin jar: $plugin_jar"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-tests)
        RUN_TESTS=0
        ;;
      --no-clean)
        CLEAN_BUILD=0
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

main() {
  parse_args "$@"

  require_cmd java
  require_cmd javac
  require_cmd jar
  require_cmd curl

  mkdir -p "$LOCAL_GRADLE_HOME"

  ensure_gradle_wrapper_jar
  build_plugin
  print_result
}

main "$@"
