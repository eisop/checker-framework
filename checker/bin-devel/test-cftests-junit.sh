#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# `./gradlew test` subsumes the other commands, but performing them
# separately seems to avoid some out-of-memory errors.
./gradlew assemble --warning-mode=all --no-build-cache
./gradlew compileTestJava testClasses --warning-mode=all --no-build-cache

if [ "$#" -eq 0 ]; then
  arg=both
elif [ "$#" -eq 1 ]; then
  arg=$1
else
  echo "$0 expects 0 or 1 arguments:" "$@"
  exit 2
fi

# The random Github Actions failures that --max-workers=1 used to work around
# (eisop#849, "internal error in type processor! method typeProcessOver()
# doesn't get called") were traced to a stale Gradle build cache reused across
# CI runs, not to test-JVM concurrency: the fix at the time was always
# `gh cache delete --all`, never reducing parallelism itself. Use
# --no-build-cache, the issue's own originally-suggested alternative, so CI
# does not read from a cache that predates the current run, while restoring
# test parallelism (--max-workers=1 was serializing all test execution).
# https://github.com/eisop/checker-framework/issues/849

## Split "test" into its parts (up to date as of 2025-11-02).
## As of 2025-11-02, :checker:test took 11.5m and everything except
## :checker:test took 15m.  (:framework:test took 6m.)
# ./gradlew test -x javadoc -x allJavadoc --warning-mode=all --no-build-cache

if [ "$arg" != "part2" ]; then
  ./gradlew junitPart1 -x javadoc -x allJavadoc --warning-mode=all --no-build-cache
fi

if [ "$arg" != "part1" ]; then
  ./gradlew junitPart2 -x javadoc -x allJavadoc --warning-mode=all --no-build-cache
fi

if [ "$arg" = "both" ]; then
  # Test clean task
  ./gradlew clean
  ./gradlew clean
fi
