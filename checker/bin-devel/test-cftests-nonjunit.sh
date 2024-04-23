#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SCRIPTDIR"/clone-related.sh

# Adding --max-workers=1 to avoid random failures in Github Actions. An alternative solution is to use --no-build-cache.
# https://github.com/eisop/checker-framework/issues/849
./gradlew nonJunitTests -x javadoc -x allJavadoc --console=plain --warning-mode=all --max-workers=1
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain --warning-mode=all
# Moved example-tests out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests -x javadoc -x allJavadoc --console=plain --warning-mode=all
