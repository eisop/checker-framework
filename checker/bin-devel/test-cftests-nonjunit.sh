#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# Adding --max-workers=1 to avoid random failures in Github Actions. An alternative solution is to use --no-build-cache.
# https://github.com/eisop/checker-framework/issues/849
./gradlew nonJunitTests -x javadoc -x allJavadoc --console=plain --warning-mode=all --max-workers=1

# Also note the test in docs/examples/publish-smoketest/ which is run
# by exampleTests below. This runs in CI, so okay to pollute local Maven.
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain --warning-mode=all

# Moved example-tests out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests -x javadoc -x allJavadoc --console=plain --warning-mode=all

# Note that test-misc also contains javadoc tests, but here we want to ensure
# allJavadoc works on all JDKs (misc is not run on every JDK).
./gradlew allJavadoc --console=plain --warning-mode=all
