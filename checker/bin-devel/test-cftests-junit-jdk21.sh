#!/bin/bash

# Compiles the Checker Framework with JDK 21, as a release is, and runs the Nullness Checker's
# JUnit tests on the JDK in $JAVA_VERSION, as a user of the release does.  This finds mismatches
# between the javac APIs that the Checker Framework is compiled against and those it runs with.
# The other JUnit tests already run on every JDK in jobs that also compile with that JDK.

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
export ORG_GRADLE_PROJECT_useJdkVersion=21
export ORG_GRADLE_PROJECT_testJdkVersion="${JAVA_VERSION:?JAVA_VERSION must name the JDK that runs the tests}"
source "$SCRIPT_DIR"/clone-related.sh

# See test-cftests-junit.sh about the build cache.
./gradlew :checker:allNullnessTests --console=plain --warning-mode=all
