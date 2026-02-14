##!/bin/bash
#
#set -e
## set -o verbose
#set -o xtrace
#export SHELLOPTS
#echo "SHELLOPTS=${SHELLOPTS}"
#
#SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
#source "$SCRIPT_DIR"/clone-related.sh
#
#./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
#
## TODO: remove uses of `main-eisop` once that becomes `main`.
#"$SCRIPT_DIR/.git-scripts/git-clone-related" --upstream-branch main-eisop eisop jspecify-reference-checker
#
#cd ../jspecify-reference-checker
#
## Delete the eisop/jdk that was already cloned...
#rm -r ../jdk
## instead clone the jspecify/jdk.
#"$SCRIPT_DIR/.git-scripts/git-clone-related" jspecify jdk
#
#JSPECIFY_CONFORMANCE_TEST_MODE=details ./gradlew build conformanceTests demoTest --console=plain --include-build "$CHECKERFRAMEWORK"
