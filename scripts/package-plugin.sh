#!/usr/bin/env bash
# [常用] 建置 Foundation、執行 Gradle 測試，並封裝 WonderlandUHC shadow jar。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_DIR="${PLUGIN_DIR:-$ROOT_DIR}"
DEFAULT_FOUNDATION_DIR="$(cd "$ROOT_DIR/.." && pwd)/lib-foundation"
FOUNDATION_DIR="${FOUNDATION_DIR:-$DEFAULT_FOUNDATION_DIR}"
FOUNDATION_VERSION="${FOUNDATION_VERSION:-6.0.8}"
FOUNDATION_JAR="$FOUNDATION_DIR/target/Foundation-${FOUNDATION_VERSION}.jar"

LOCAL_M2_REPO="${LOCAL_M2_REPO:-$ROOT_DIR/.m2-local}"
LOCAL_GRADLE_HOME="${LOCAL_GRADLE_HOME:-$ROOT_DIR/.gradle-local}"

RUN_TESTS=1
CLEAN_BUILD=1
BUILD_FOUNDATION=1
BOOTSTRAP_DEPS=1

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
  --skip-foundation  Reuse the existing lib-foundation target jar.
  --skip-bootstrap   Do not refresh Foundation compatibility aliases.
  -h, --help         Show this help.

Environment:
  PLUGIN_DIR         Plugin repository directory. Default: this repository.
  FOUNDATION_DIR     Foundation repository directory. Default: ../lib-foundation.
  FOUNDATION_VERSION Foundation jar version. Default: 6.0.8.
  LOCAL_M2_REPO       Maven repository used for local compatibility aliases.
  LOCAL_GRADLE_HOME   Gradle user home used by this repository.
  GRADLE_EXTRA_ARGS   Extra arguments appended to ./gradlew.
  MAVEN_EXTRA_ARGS    Extra arguments appended to the Foundation mvn command.
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

  log "gradle-wrapper.jar missing; downloading Gradle 6.9 wrapper jar."
  curl -sSL --fail \
    -o "$wrapper_jar" \
    "https://raw.githubusercontent.com/gradle/gradle/v6.9.4/gradle/wrapper/gradle-wrapper.jar"
}

bootstrap_dependencies() {
  if [[ "$BOOTSTRAP_DEPS" != "1" ]]; then
    return 0
  fi

  log "Refreshing Foundation compatibility aliases."
  LOCAL_M2_REPO="$LOCAL_M2_REPO" "$ROOT_DIR/scripts/bootstrap-foundation-deps.sh"
}

build_foundation() {
  if [[ "$BUILD_FOUNDATION" != "1" ]]; then
    if [[ ! -f "$FOUNDATION_JAR" ]]; then
      echo "Missing Foundation jar: $FOUNDATION_JAR" >&2
      exit 1
    fi
    log "Reusing existing Foundation jar: $FOUNDATION_JAR"
    return 0
  fi

  log "Building lib-foundation."
  if [[ ! -d "$FOUNDATION_DIR" ]]; then
    echo "Foundation directory does not exist: $FOUNDATION_DIR" >&2
    echo "Use FOUNDATION_DIR=/path/to/lib-foundation or --skip-foundation if the jar already exists." >&2
    exit 1
  fi

  (
    cd "$FOUNDATION_DIR"
    local -a maven_args=(
      -DskipTests
      -Dmaven.javadoc.skip=true
      "-Dmaven.repo.local=$LOCAL_M2_REPO"
      package
    )

    if [[ -n "${MAVEN_EXTRA_ARGS:-}" ]]; then
      local -a extra_args
      read -r -a extra_args <<<"$MAVEN_EXTRA_ARGS"
      maven_args+=("${extra_args[@]}")
    fi

    mvn "${maven_args[@]}"
  )

  if [[ ! -f "$FOUNDATION_JAR" ]]; then
    echo "Foundation build completed but jar was not found: $FOUNDATION_JAR" >&2
    exit 1
  fi
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

    gradle_args+=(shadowJar -PuseLocalFoundation --no-daemon -Dorg.gradle.native=false)

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
      --skip-foundation)
        BUILD_FOUNDATION=0
        ;;
      --skip-bootstrap)
        BOOTSTRAP_DEPS=0
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
  require_cmd mvn
  require_cmd curl

  mkdir -p "$LOCAL_M2_REPO" "$LOCAL_GRADLE_HOME"

  ensure_gradle_wrapper_jar
  bootstrap_dependencies
  build_foundation
  build_plugin
  print_result
}

main "$@"
