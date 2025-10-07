#!/bin/bash

# JTReg Test Harness Setup Script
#
# This script downloads and configures the JTReg test harness required for
# Checker Framework performance benchmarks. JTReg is the official OpenJDK
# regression test harness used for testing Java compiler implementations.
#
# Usage: Run from the checker/harness directory:
#   bash setup-jtreg.sh
#
# The script will install JTReg in the appropriate location relative to the
# project structure to ensure compatibility with the performance test suite.

set -e

# JTReg version configuration
readonly JTREG_VERSION="7.5"
readonly JTREG_BUILD="b01"
readonly JTREG_DOWNLOAD_URL="https://builds.shipilev.net/jtreg/jtreg-${JTREG_VERSION}+${JTREG_BUILD}.tar.gz"

# Path resolution: from checker/harness to project structure
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly CF_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"  # checker-framework root
readonly INSTALL_PARENT="$(dirname "$CF_ROOT")"          # parent of checker-framework
readonly JTREG_INSTALL_PATH="${INSTALL_PARENT}/jtreg"

echo "=========================================="
echo "JTReg Test Harness Setup v${JTREG_VERSION}"
echo "=========================================="
echo "Project root: ${CF_ROOT}"
echo "Install target: ${JTREG_INSTALL_PATH}"
echo ""

# Validate project structure
if [[ ! -d "${CF_ROOT}/checker" ]]; then
    echo "ERROR: Invalid project structure. Expected checker-framework root at: ${CF_ROOT}"
    echo "Please run this script from the checker/harness directory."
    exit 1
fi

# Check for existing installation
if [[ -f "${JTREG_INSTALL_PATH}/bin/jtreg" ]]; then
    echo "✓ JTReg is already installed at: ${JTREG_INSTALL_PATH}"

    # Display version information if available
    if [[ -f "${JTREG_INSTALL_PATH}/release" ]]; then
        echo ""
        echo "Current installation:"
        cat "${JTREG_INSTALL_PATH}/release" | sed 's/^/  /'
    fi

    echo ""
    echo "To reinstall, remove the existing directory:"
    echo "  rm -rf \"${JTREG_INSTALL_PATH}\""
    exit 0
fi

# Create secure temporary workspace
readonly TMP_WORKSPACE=$(mktemp -d)
trap "rm -rf '${TMP_WORKSPACE}'" EXIT

echo "→ Downloading JTReg ${JTREG_VERSION}+${JTREG_BUILD}..."
cd "${TMP_WORKSPACE}"

# Download with fallback mechanisms
if command -v curl >/dev/null 2>&1; then
    if ! curl -L --fail -o jtreg.tar.gz "${JTREG_DOWNLOAD_URL}"; then
        echo "ERROR: Download failed using curl"
        exit 1
    fi
elif command -v wget >/dev/null 2>&1; then
    if ! wget -O jtreg.tar.gz "${JTREG_DOWNLOAD_URL}"; then
        echo "ERROR: Download failed using wget"
        exit 1
    fi
else
    echo "ERROR: Neither curl nor wget is available"
    echo ""
    echo "Manual installation required:"
    echo "1. Download: ${JTREG_DOWNLOAD_URL}"
    echo "2. Extract to: ${JTREG_INSTALL_PATH}"
    echo "3. Ensure executables have proper permissions"
    exit 1
fi

echo "→ Extracting JTReg archive..."
if ! tar -xzf jtreg.tar.gz; then
    echo "ERROR: Failed to extract JTReg archive"
    exit 1
fi

# Locate extracted directory (handles version-specific naming)
readonly EXTRACTED_JTREG=$(find . -maxdepth 1 -type d -name "jtreg*" | head -1)
if [[ -z "${EXTRACTED_JTREG}" ]]; then
    echo "ERROR: JTReg directory not found after extraction"
    exit 1
fi

echo "→ Installing JTReg to target location..."
if ! mv "${EXTRACTED_JTREG}" "${JTREG_INSTALL_PATH}"; then
    echo "ERROR: Failed to move JTReg to installation directory"
    exit 1
fi

# Configure executable permissions
echo "→ Configuring JTReg executables..."
chmod +x "${JTREG_INSTALL_PATH}/bin/jtreg" 2>/dev/null || true
chmod +x "${JTREG_INSTALL_PATH}/bin/jtdiff" 2>/dev/null || true

# Installation verification
if [[ -f "${JTREG_INSTALL_PATH}/bin/jtreg" ]]; then
    echo ""
    echo "=========================================="
    echo "✓ JTReg installation completed successfully"
    echo "=========================================="
    echo "Installation path: ${JTREG_INSTALL_PATH}"

    # Display version information
    if [[ -f "${JTREG_INSTALL_PATH}/release" ]]; then
        echo ""
        echo "JTReg version information:"
        cat "${JTREG_INSTALL_PATH}/release" | sed 's/^/  /'
    fi

    echo ""
    echo "The performance test suite is now ready for execution."
    echo "Example usage from checker/harness directory:"
    echo ""
    echo "  ../../gradlew :harness-driver-cli:run --args=\"\\"
    echo "    --engine jtreg \\"
    echo "    --jtreg ../../../jtreg/bin \\"
    echo "    --generator NewAndArray \\"
    echo "    --processor org.checkerframework.checker.nullness.NullnessChecker \\"
    echo "    ... <additional parameters>\""
    echo ""
else
    echo "ERROR: JTReg installation verification failed"
    echo "Expected executable not found: ${JTREG_INSTALL_PATH}/bin/jtreg"
    exit 1
fi
