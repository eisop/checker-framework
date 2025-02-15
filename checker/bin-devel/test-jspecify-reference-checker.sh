#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh
./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

GIT_SCRIPTS="$SCRIPTDIR/.git-scripts"
# TODO: remove uses of `main-eisop` once that becomes `main`.
"$GIT_SCRIPTS/git-clone-related" --upstream-branch main-eisop eisop jspecify-reference-checker

cd ../jspecify-reference-checker

# Delete the eisop/jdk that was already cloned...
rm -r ../jdk
# instead clone the jspecify/jdk.
"$GIT_SCRIPTS/git-clone-related" jspecify jdk

JSPECIFY_CONFORMANCE_TEST_MODE=details ./gradlew build conformanceTests demoTest --console=plain --include-build "$CHECKERFRAMEWORK"
