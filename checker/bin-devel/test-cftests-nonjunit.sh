#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
source "$SCRIPTDIR"/clone-related.sh


./gradlew nonJunitTests -x javadoc -x allJavadoc --console=plain --warning-mode=all --info --stacktrace --scan
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain --warning-mode=all
# Moved example-tests out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests -x javadoc -x allJavadoc --console=plain --warning-mode=all
