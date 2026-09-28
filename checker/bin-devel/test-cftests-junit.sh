#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# The random Github Actions failures that --max-workers=1 used to work around
# (eisop#849, "internal error in type processor! method typeProcessOver()
# doesn't get called") were traced to a stale Gradle build cache reused across
# CI runs, not to test-JVM concurrency: the fix at the time was always
# `gh cache delete --all`, never reducing parallelism itself. Use
# --no-build-cache, the issue's own originally-suggested alternative, so CI
# does not read from a cache that predates the current run, while restoring
# test parallelism (--max-workers=1 was serializing all test execution).
# https://github.com/eisop/checker-framework/issues/849
./gradlew test -x javadoc -x allJavadoc --console=plain --warning-mode=all --no-build-cache

# Test clean task
./gradlew clean
./gradlew clean
