#!/usr/bin/env bash
#
# Downloads the Forge 1.12.2 MDK, copies our source over it, replaces the
# stock build.gradle with ours. Result: a buildable workspace at $MDK_DIR.
#
# Designed for CI but also runnable locally. Idempotent: safe to re-run; will
# wipe and re-extract $MDK_DIR.
#
# Usage:
#     ci/setup-mdk.sh [mdk-dir]
#
# Defaults: mdk-dir = "mdk"
#
# Env knobs:
#   FORGE_MDK_URL  override the MDK download URL (default: 14.23.5.2860)
#   MOD_VERSION    passed through as -PmodVersion to gradle later if needed
#
set -euo pipefail

MDK_DIR="${1:-mdk}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FORGE_MDK_URL="${FORGE_MDK_URL:-https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-mdk.zip}"

echo "[setup-mdk] Repo root: $REPO_ROOT"
echo "[setup-mdk] MDK dir:   $MDK_DIR"

if [[ -d "$MDK_DIR" ]]; then
    echo "[setup-mdk] Removing existing $MDK_DIR"
    rm -rf "$MDK_DIR"
fi

echo "[setup-mdk] Downloading Forge MDK"
curl -fSL "$FORGE_MDK_URL" -o /tmp/forge-mdk.zip

echo "[setup-mdk] Extracting"
mkdir -p "$MDK_DIR"
unzip -q -o /tmp/forge-mdk.zip -d "$MDK_DIR"

echo "[setup-mdk] Removing example mod"
rm -rf "$MDK_DIR/src/main/java/com/example"

echo "[setup-mdk] Copying our source in"
mkdir -p "$MDK_DIR/src/main/java/com"
cp -R "$REPO_ROOT/src/main/java/com/flashminat0" "$MDK_DIR/src/main/java/com/"
cp -R "$REPO_ROOT/src/test" "$MDK_DIR/src/"
cp "$REPO_ROOT/src/main/resources/mcmod.info" "$MDK_DIR/src/main/resources/mcmod.info"

echo "[setup-mdk] Replacing stock build.gradle with ours"
cp "$REPO_ROOT/ci/build.gradle" "$MDK_DIR/build.gradle"

echo "[setup-mdk] Done. Run gradle from $MDK_DIR:"
echo "    cd $MDK_DIR && ./gradlew test build"
