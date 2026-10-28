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
./gradlew assemble --warning-mode=all
./gradlew compileTestJava testClasses --warning-mode=all

if [ "$#" -eq 0 ]; then
  arg=both
elif [ "$#" -eq 1 ]; then
  arg=$1
else
  echo "$0 expects 0 or 1 arguments:" "$@"
  exit 2
fi

# These scripts use the Gradle build cache, which on CI is the cache that master's runs save
# (setup-gradle restores it read-only for pull requests).  Test tasks are never loaded from it:
# they run every time.  Earlier, --max-workers=1 and then worked around random
# "internal error in type processor! method typeProcessOver() doesn't get called" failures that
# were attributed to a stale build cache but never reproduced or explained (eisop#849).  If they
# return, CFGVisualizeLauncher now reports the real exception.
# https://github.com/eisop/checker-framework/issues/849

## Split "test" into its parts (up to date as of 2025-11-02).
## As of 2025-11-02, :checker:test took 11.5m and everything except
## :checker:test took 15m.  (:framework:test took 6m.)
# ./gradlew test -x javadoc -x allJavadoc --warning-mode=all

if [ "$arg" != "part2" ]; then
  ./gradlew junitPart1 -x javadoc -x allJavadoc --warning-mode=all
fi

if [ "$arg" != "part1" ]; then
  ./gradlew junitPart2 -x javadoc -x allJavadoc --warning-mode=all
fi

# Part 2 is the shorter part, so it runs the clean check when the parts are run separately.
if [ "$arg" != "part1" ]; then
  # Test clean task
  ./gradlew clean
  ./gradlew clean
fi
