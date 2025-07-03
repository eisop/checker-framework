#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
# Test that the CF, when built with JDK 21, works on other JDKs.
export ORG_GRADLE_PROJECT_useJdk21Compiler=true
source "$SCRIPT_DIR"/clone-related.sh

# Adding --max-workers=1 to avoid random failures in Github Actions. An alternative solution is to use --no-build-cache.
# https://github.com/eisop/checker-framework/issues/849
./gradlew test -x javadoc -x allJavadoc --console=plain --warning-mode=all --max-workers=1

# Test clean task
./gradlew clean
./gradlew clean
