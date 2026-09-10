#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# See test-cftests-junit.sh for why this is --no-build-cache rather than
# --max-workers=1.
# https://github.com/eisop/checker-framework/issues/849
./gradlew nonJunitTests -x javadoc -x allJavadoc --console=plain --warning-mode=all --no-build-cache

# Also note the test in docs/examples/publish-smoketest/ which is run
# by exampleTests below. This runs in CI, so okay to pollute local Maven.
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain --warning-mode=all

# Moved example-tests out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests -x javadoc -x allJavadoc --console=plain --warning-mode=all

# Note that test-misc also contains javadoc tests, but here we want to ensure
# allJavadoc works on all JDKs (misc is not run on every JDK).
./gradlew allJavadoc --console=plain --warning-mode=all
