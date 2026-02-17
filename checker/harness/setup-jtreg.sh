#!/usr/bin/env bash
# JTReg Test Harness Setup Script
#
# Usage (run from checker/harness):
#   bash setup-jtreg.sh
#
# Notes:
# - Installs JTReg alongside checker-framework at ../jtreg so that
#   relative paths like ../../../jtreg/bin/jtreg work in scripts/docs.
# - Downloads prebuilt archives from Shipilev builds.
# - Override defaults via environment variables: JTREG_VERSION, JTREG_BUILD
#   e.g., JTREG_VERSION=7.4 JTREG_BUILD=1 bash setup-jtreg.sh

set -euo pipefail

# -------- Version / URLs --------
JTREG_VERSION="${JTREG_VERSION:-7.4}"
JTREG_BUILD="${JTREG_BUILD:-1}"

# Shipilev provides versioned zips like jtreg-7.4+1.zip
JTREG_ARCHIVE="jtreg-${JTREG_VERSION}+${JTREG_BUILD}.zip"
JTREG_URL_PRIMARY="https://builds.shipilev.net/jtreg/${JTREG_ARCHIVE}"
# Fallback (rolling zip; may change over time—use only if primary fails)
JTREG_URL_FALLBACK="https://builds.shipilev.net/jtreg/jtreg.zip"

# -------- Path resolution (run from checker/harness) --------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Repository root (contains checker/ etc.)
CF_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
# Install to the parent of checker-framework: <parent>/jtreg
INSTALL_PARENT="$(dirname "$CF_ROOT")"
JTREG_INSTALL_PATH="${INSTALL_PARENT}/jtreg"

echo "=========================================="
echo "JTReg Test Harness Setup v${JTREG_VERSION}+${JTREG_BUILD}"
echo "=========================================="
echo "Project root: ${CF_ROOT}"
echo "Install target: ${JTREG_INSTALL_PATH}"
echo ""

# -------- Sanity check: repository layout --------
if [[ ! -d "${CF_ROOT}/checker" ]]; then
  echo "ERROR: Invalid repository layout."
  echo "Run this script from checker/harness within checker-framework."
  exit 1
fi

# -------- Existing installation --------
if [[ -x "${JTREG_INSTALL_PATH}/bin/jtreg" ]]; then
  echo "✓ JTReg already installed at: ${JTREG_INSTALL_PATH}"
  echo ""
  if [[ -f "${JTREG_INSTALL_PATH}/release" ]]; then
    echo "Current installation info:"
    sed 's/^/  /' "${JTREG_INSTALL_PATH}/release" || true
  fi
  echo ""
  echo "To reinstall, remove the existing directory first:"
  echo "  rm -rf \"${JTREG_INSTALL_PATH}\""
  exit 0
fi

# -------- Tooling checks --------
need_cmd() {
  command -v "$1" >/dev/null 2>&1
}
if ! need_cmd unzip; then
  echo "ERROR: unzip is required but not found on PATH."
  echo "Install unzip and retry."
  exit 1
fi
if ! need_cmd curl && ! need_cmd wget; then
  echo "ERROR: Neither curl nor wget is available."
  echo "Manual steps:"
  echo "  1) Download: ${JTREG_URL_PRIMARY} (or ${JTREG_URL_FALLBACK})"
  echo "  2) Extract to: ${JTREG_INSTALL_PATH}"
  exit 1
fi

# -------- Temp workspace --------
TMP_WORKSPACE="$(mktemp -d)"
trap 'rm -rf "${TMP_WORKSPACE}"' EXIT

echo "→ Downloading JTReg ${JTREG_VERSION}+${JTREG_BUILD} ..."
echo "  URL: ${JTREG_URL_PRIMARY}"
cd "${TMP_WORKSPACE}"

download_ok=false
outfile="jtreg.zip"

fetch() {
  local url="$1"
  if need_cmd curl; then
    curl -L --fail -o "${outfile}" "${url}"
  else
    wget -O "${outfile}" "${url}"
  fi
}

if fetch "${JTREG_URL_PRIMARY}"; then
  download_ok=true
else
  echo "WARN: Primary download failed; trying fallback ..."
  if fetch "${JTREG_URL_FALLBACK}"; then
    download_ok=true
  fi
fi

if [[ "${download_ok}" != true ]]; then
  echo "ERROR: Could not download JTReg (all URLs failed)."
  echo "Manual steps:"
  echo "  1) Download: ${JTREG_URL_PRIMARY} (or ${JTREG_URL_FALLBACK})"
  echo "  2) Extract to: ${JTREG_INSTALL_PATH}"
  exit 1
fi

echo "→ Extracting archive ..."
unzip -q "${outfile}"

# Robust directory match (exclude .)
EXTRACTED_JTREG="$(find . -mindepth 1 -maxdepth 1 -type d -name "jtreg*" | head -1 || true)"
if [[ -z "${EXTRACTED_JTREG}" ]]; then
  echo "ERROR: Extracted JTReg directory not found."
  exit 1
fi

echo "→ Installing to ${JTREG_INSTALL_PATH} ..."
mkdir -p "$(dirname "${JTREG_INSTALL_PATH}")"
mv "${EXTRACTED_JTREG}" "${JTREG_INSTALL_PATH}"

echo "→ Setting executable permissions ..."
chmod +x "${JTREG_INSTALL_PATH}/bin/jtreg" 2>/dev/null || true
chmod +x "${JTREG_INSTALL_PATH}/bin/jtdiff" 2>/dev/null || true

# Optional: clear macOS Gatekeeper quarantine flags
if [[ "$(uname -s)" == "Darwin" ]]; then
  xattr -dr com.apple.quarantine "${JTREG_INSTALL_PATH}" 2>/dev/null || true
fi

# -------- Verify --------
if [[ -x "${JTREG_INSTALL_PATH}/bin/jtreg" ]]; then
  echo ""
  echo "=========================================="
  echo "✓ JTReg installation completed"
  echo "=========================================="
  echo "Installation path: ${JTREG_INSTALL_PATH}"
  echo ""
  if [[ -f "${JTREG_INSTALL_PATH}/release" ]]; then
    echo "JTReg release file:"
    sed 's/^/  /' "${JTREG_INSTALL_PATH}/release" || true
    echo ""
  fi
  echo "Version check:"
  "${JTREG_INSTALL_PATH}/bin/jtreg" -version 2>/dev/null || "${JTREG_INSTALL_PATH}/bin/jtreg" --version || true
  echo ""
  echo "Example (run from checker/harness):"
  cat <<'EOF'
  ../../gradlew :harness-driver-cli:run --no-daemon --console=plain --args="\
    --engine jtreg \
    --jtreg ../../jtreg/bin \
    --jtreg-test checker/harness/jtreg/JtregPerfHarness.java \
    --generator NewAndArray \
    --processor org.checkerframework.checker.nullness.NullnessChecker \
    ..."
EOF
else
  echo "ERROR: JTReg installation verification failed."
  echo "Missing executable: ${JTREG_INSTALL_PATH}/bin/jtreg"
  exit 1
fi
