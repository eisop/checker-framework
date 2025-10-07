#!/usr/bin/env bash
# JTReg Test Harness Setup Script
#
# Usage (run from checker/harness):
#   bash setup-jtreg.sh
#
# Notes:
# - Installs JTReg alongside checker-framework at ../jtreg so that
#   relative paths like ../../../jtreg/bin/jtreg work in scripts/docs.
# - Downloads the official GitHub release (the tag’s “+” must be URL-encoded).
# - Override defaults via environment variables: JTREG_VERSION, JTREG_BUILD
#   e.g., JTREG_VERSION=7.4 JTREG_BUILD=1 bash setup-jtreg.sh

set -euo pipefail

# -------- Version / URLs --------
JTREG_VERSION="${JTREG_VERSION:-7.4}"
JTREG_BUILD="${JTREG_BUILD:-1}"

# GitHub release tags are like jtreg-7.4+1 (the + must be %2B in URLs).
JTREG_TAG_ENC="jtreg-${JTREG_VERSION}%2B${JTREG_BUILD}"
JTREG_ARCHIVE="jtreg-${JTREG_VERSION}+${JTREG_BUILD}.tar.gz"
JTREG_URL_GH="https://github.com/openjdk/jtreg/releases/download/${JTREG_TAG_ENC}/${JTREG_ARCHIVE}"

# Optional alternate source (may not exist for all versions; keep for manual use)
# JTREG_URL_ALT="https://builds.shipilev.net/jtreg/jtreg-${JTREG_VERSION}+b${JTREG_BUILD}.tar.gz"

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

# -------- Temp workspace --------
TMP_WORKSPACE="$(mktemp -d)"
trap 'rm -rf "${TMP_WORKSPACE}"' EXIT

echo "→ Downloading JTReg ${JTREG_VERSION}+${JTREG_BUILD} ..."
echo "  URL: ${JTREG_URL_GH}"
cd "${TMP_WORKSPACE}"

download_ok=false
if command -v curl >/dev/null 2>&1; then
  # -L follow redirects; --fail causes 4xx/5xx to return nonzero exit
  if curl -L --fail -o jtreg.tar.gz "${JTREG_URL_GH}"; then
    download_ok=true
  fi
elif command -v wget >/dev/null 2>&1; then
  if wget -O jtreg.tar.gz "${JTREG_URL_GH}"; then
    download_ok=true
  fi
else
  echo "ERROR: Neither curl nor wget is available."
  echo "Manual steps:"
  echo "  1) Download: ${JTREG_URL_GH}"
  echo "  2) Extract to: ${JTREG_INSTALL_PATH}"
  exit 1
fi

if [[ "${download_ok}" != true ]]; then
  echo "ERROR: Download from GitHub failed."
  # To enable the alternate source, uncomment below and set JTREG_URL_ALT as needed.
  # echo "Trying alternate URL: ${JTREG_URL_ALT}"
  # if curl -L --fail -o jtreg.tar.gz "${JTREG_URL_ALT}"; then
  #   download_ok=true
  # fi
  # if [[ "${download_ok}" != true ]]; then
  #   echo "ERROR: Alternate download also failed."
  #   exit 1
  # fi
  exit 1
fi

echo "→ Extracting archive ..."
tar -xzf jtreg.tar.gz

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
