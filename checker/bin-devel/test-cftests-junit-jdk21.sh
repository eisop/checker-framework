#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
export ORG_GRADLE_PROJECT_useJdkVersion=21
source "$SCRIPT_DIR"/clone-related.sh

# See test-cftests-junit.sh for why this is --no-build-cache rather than
# --max-workers=1.
# https://github.com/eisop/checker-framework/issues/849
./gradlew test -x javadoc -x allJavadoc --console=plain --warning-mode=all --no-build-cache
