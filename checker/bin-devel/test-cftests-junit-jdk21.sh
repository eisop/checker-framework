#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
export ORG_GRADLE_PROJECT_useJdkVersion=21
source "$SCRIPT_DIR"/clone-related.sh

# Adding --max-workers=1 to avoid random failures in Github Actions. An alternative solution is to use --no-build-cache.
# https://github.com/eisop/checker-framework/issues/849
./gradlew test -x javadoc -x allJavadoc --console=plain --warning-mode=all --max-workers=1
