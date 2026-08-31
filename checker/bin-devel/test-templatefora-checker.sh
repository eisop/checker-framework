#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

"$SCRIPT_DIR/.git-scripts/git-clone-related" eisop templatefora-checker

cd ../templatefora-checker

# --include-build makes Gradle substitute this checkout's own :checker, :checker-qual, and
# :framework-test project outputs for the io.github.eisop:checker, :checker-qual, and
# :framework-test coordinates templatefora-checker's build.gradle declares, automatically:
# both share the "io.github.eisop" group and matching project/module names, which is all
# Gradle's composite-build dependency substitution needs. This tests this checkout's own
# in-progress state, not the last eisop release -- see jspecify-reference-checker's own use of
# this same mechanism, and its checker-framework/build.gradle "checkerFramework" ext property,
# for a more elaborate example.
./gradlew build --console=plain --warning-mode=all --include-build "$CHECKERFRAMEWORK" \
  --no-configuration-cache \
  -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
